/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2011 ComtecSF S.L.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.libreplan.web.users.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.libreplan.business.common.IAdHocTransactionService;
import org.libreplan.business.common.IOnTransaction;
import org.libreplan.business.common.daos.IConfigurationDAO;
import org.libreplan.business.common.entities.ConfigurationRolesLDAP;
import org.libreplan.business.common.entities.LDAPConfiguration;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.users.daos.IUserDAO;
import org.libreplan.business.users.entities.User;
import org.libreplan.business.users.entities.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;

/**
 * An extending from AbstractUserDetailsAuthenticationProvider class which is
 * used to implement the authentication against LDAP.
 *
 * This provider implements the process explained in
 * <https://wiki.libreplan.org/twiki/bin/view/LibrePlan/AnA04S06LdapAuthentication>
 *
 * At this time it authenticates user against LDAP and then searches it in BD to
 * use the BD user in application.
 *
 * @author Ignacio Diaz Teijido <ignacio.diaz@comtecsf.es>
 * @author Cristina Alvarino Perez <cristina.alvarino@comtecsf.es>
 *
 */

// TODO resolve deprecated methods
public class LDAPCustomAuthenticationProvider
        extends AbstractUserDetailsAuthenticationProvider
        implements AuthenticationProvider {

    @Autowired
    private IAdHocTransactionService transactionService;

    @Autowired
    private IConfigurationDAO configurationDAO;

    @Autowired
    private IUserDAO userDAO;

    private LDAPConfiguration configuration;

    /** Template to search in LDAP */
    private LdapTemplate ldapTemplate;

    private UserDetailsService userDetailsService;

    private DBPasswordEncoderService passwordEncoderService;

    private static final String COLON = ":";

    private static final String USER_ID_SUBSTITUTION = "[USER_ID]";

    private static final Log LOG = LogFactory.getLog(LDAPCustomAuthenticationProvider.class);

    /**
     * LDAP role matching could be configured using an asterix (*) to specify all users or groups
     */
    private static final String WILDCHAR_ALL = "*";

    @Override
    protected void additionalAuthenticationChecks(UserDetails arg0, UsernamePasswordAuthenticationToken arg1) {
        // No needed at this time
    }

    @Transactional(readOnly = true)
    @Override
    public UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) {

        String clearPassword = authentication.getCredentials().toString();

        if ( StringUtils.isBlank(username) || StringUtils.isBlank(clearPassword) ) {
            throw new BadCredentialsException("Username and password can not be empty");
        }

        String encodedPassword = passwordEncoderService.encodePassword(clearPassword, username);
        User user = getUserFromDB(username);

        // If user != null then exists in LibrePlan
        if ( null != user && user.isLibrePlanUser() ) {
            // is a LibrePlan user, then we must authenticate against DB
            return authenticateInDatabase(username, user, encodedPassword);
        }

        // If it's a LDAP or null user, then we must authenticate against LDAP

        // Load LDAPConfiguration properties
        configuration = loadLDAPConfiguration();

        if ( configuration.getLdapAuthEnabled() ) {
            // Sets the new context to ldapTemplate
            ldapTemplate.setContextSource(loadLDAPContext());

            try {

                // Test authentication for user against LDAP
                if ( authenticateAgainstLDAP(username, clearPassword) ) {

                    // Authentication against LDAP was ok
                    if ( null == user ) {

                        // User does not exist in LibrePlan must be imported
                        user = createLDAPUserWithRoles(username, encodedPassword);
                    } else {

                        // Update password
                        if ( configuration.isLdapSavePasswordsDB() ) {
                            user.setPassword(encodedPassword);
                        }

                        // Update roles from LDAP
                        setRoles(user);
                    }
                    saveUserOnTransaction(user);

                    return loadUserDetails(username);
                } else {
                    throw new BadCredentialsException("User is not in LDAP.");
                }
            } catch (Exception e) {
                // This exception captures when LDAP authentication is not possible
                LOG.info("LDAP not reachable. Trying to authenticate against database.", e);
            }
        }

        // LDAP is not enabled we must check if the LDAP user is in DB
        return authenticateInDatabase(username, user, encodedPassword);
    }

    private UserDetails loadUserDetails(String username) {
        return getUserDetailsService().loadUserByUsername(username);
    }

    private void setRoles(User user) {
        if ( configuration.getLdapSaveRolesDB() ) {
            user.clearRoles();
            List<String> roles = getMatchedRoles(configuration, user.getLoginName());
            for (String role : roles) {
                user.addRole(UserRole.valueOf(UserRole.class, role));
            }
        }
    }

    private User createLDAPUserWithRoles(String username, String encodedPassword) {
        User user = User.create();
        user.setLoginName(username);

        String newEncodedPassword = encodedPassword;

        // we must check if it is needed to save LDAP passwords in DB
        if ( !configuration.isLdapSavePasswordsDB() ) {
            newEncodedPassword = null;
        }

        user.setPassword(newEncodedPassword);
        user.setLibrePlanUser(false);
        user.setDisabled(false);
        setRoles(user);

        return user;
    }

    private LDAPConfiguration loadLDAPConfiguration() {
        return transactionService.runOnReadOnlyTransaction(new IOnTransaction<LDAPConfiguration>() {
            @Override
            public LDAPConfiguration execute() {
                return configurationDAO.getConfiguration().getLdapConfiguration();
            }
        });
    }

    private User getUserFromDB(String username) {
        final String usernameInserted = username;

        return transactionService.runOnReadOnlyTransaction(new IOnTransaction<User>() {
            @Override
            public User execute() {
                try {
                    return userDAO.findByLoginName(usernameInserted);
                } catch (InstanceNotFoundException e) {
                    LOG.info("User " + usernameInserted + " not found in database.");
                    return null;
                }
            }
        });

    }

    private LDAPCustomContextSource loadLDAPContext() {

        // Establishes the context for LDAP connection.
        LDAPCustomContextSource context = (LDAPCustomContextSource) ldapTemplate.getContextSource();

        context.setUrl(configuration.getLdapHost() + COLON + configuration.getLdapPort());
        context.setBase(configuration.getLdapBase());
        context.setUserDn(configuration.getLdapUserDn());
        context.setPassword(configuration.getLdapPassword());

        try {
            context.afterPropertiesSet();
        } catch (Exception e) {
            // This exception will be never reached if the LDAP properties are well-formed.
            LOG.error("There is a problem in LDAP connection: ", e);
        }

        return context;
    }

    private boolean authenticateAgainstLDAP(String username, String clearPassword) {
        return ldapTemplate.authenticate(
                DistinguishedName.EMPTY_PATH,
                new EqualsFilter(configuration.getLdapUserId(), username).toString(),
                clearPassword);
    }

    private void saveUserOnTransaction(User user) {
        final User librePlanUser = user;

        transactionService.runOnTransaction(new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                userDAO.save(librePlanUser);
                return null;
            }
        });
    }

    private UserDetails authenticateInDatabase(String username, User user, String encodedPassword) {
        if ( null != user && null != user.getPassword() && encodedPassword.equals(user.getPassword()) ) {
            return loadUserDetails(username);
        } else {
            throw new BadCredentialsException("Credentials are not the same as in database.");
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getRolesUsingNodeStrategy(
            Set<ConfigurationRolesLDAP> rolesLdap, String queryRoles, final LDAPConfiguration configuration) {

        String roleProperty = configuration.getLdapRoleProperty();

        List<String> rolesReturn = new ArrayList<>();
        for (ConfigurationRolesLDAP roleLDAP : rolesLdap) {
            if ( roleLDAP.getRoleLdap().equals(WILDCHAR_ALL) ) {
                rolesReturn.add(roleLDAP.getRoleLibreplan());
                continue;
            }

            // We must make a search for each role-matching in nodes
            List<Attribute> resultsSearch = new ArrayList<>();
            resultsSearch.addAll(ldapTemplate.search(
                    DistinguishedName.EMPTY_PATH,
                    new EqualsFilter(roleProperty, roleLDAP.getRoleLdap()).toString(),
                    new AttributesMapper() {
                        @Override
                        public Object mapFromAttributes(Attributes attributes) throws NamingException {
                            return attributes.get(configuration.getLdapUserId());
                        }
                    }));

            for (Attribute atrib : resultsSearch) {
                if ( atrib.contains(queryRoles) ) {
                    rolesReturn.add(roleLDAP.getRoleLibreplan());
                }
            }
        }

        return rolesReturn;
    }

    private List<String> getRolesUsingBranchStrategy(
            Set<ConfigurationRolesLDAP> rolesLdap, String queryRoles, LDAPConfiguration configuration) {

        String roleProperty = configuration.getLdapRoleProperty();
        String groupsPath = configuration.getLdapGroupPath();

        List<String> rolesReturn = new ArrayList<>();

        for (ConfigurationRolesLDAP roleLdap : rolesLdap) {
            if ( roleLdap.getRoleLdap().equals(WILDCHAR_ALL) ) {
                rolesReturn.add(roleLdap.getRoleLibreplan());
                continue;
            }

            // We must make a search for each role matching
            DirContextAdapter adapter = null;
            try {
                adapter = (DirContextAdapter) ldapTemplate.lookup(roleLdap.getRoleLdap() + "," + groupsPath);
            } catch (org.springframework.ldap.NamingException ne) {
                LOG.error(ne.getMessage());
            }
            if ( adapter != null && adapter.attributeExists(roleProperty) ) {
                Attributes atrs = adapter.getAttributes();

                if ( atrs.get(roleProperty).contains(queryRoles) ) {
                    rolesReturn.add(roleLdap.getRoleLibreplan());
                }
            }
        }

        return rolesReturn;
    }

    private List<String> getMatchedRoles(LDAPConfiguration configuration, String username) {

        String queryRoles = configuration.getLdapSearchQuery().replace(USER_ID_SUBSTITUTION, username);

        Set<ConfigurationRolesLDAP> rolesLdap = configuration.getConfigurationRolesLdap();

        try {
            if ( !configuration.getLdapGroupStrategy() ) {
                // The LDAP has a node strategy for groups, we must check the roleProperty in user node
                return getRolesUsingNodeStrategy(rolesLdap, queryRoles, configuration);
            } else {
                // The LDAP has a branch strategy for groups we must check if the user is in one of the groups
                return getRolesUsingBranchStrategy(rolesLdap, queryRoles, configuration);
            }
        } catch (Exception e) {
            LOG.error("Configuration of LDAP role-matching is wrong. Please check it.", e);
            return Collections.emptyList();
        }
    }

    public DBPasswordEncoderService getPasswordEncoderService() {
        return passwordEncoderService;
    }

    public void setPasswordEncoderService(DBPasswordEncoderService passwordEncoderService) {
        this.passwordEncoderService = passwordEncoderService;
    }

    public LdapTemplate getLdapTemplate() {
        return ldapTemplate;
    }

    public void setLdapTemplate(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }

    public void setUserDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    public UserDetailsService getUserDetailsService() {
        return userDetailsService;
    }

}
