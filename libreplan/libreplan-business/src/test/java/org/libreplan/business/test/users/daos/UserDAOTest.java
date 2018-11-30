/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 * Copyright (C) 2010-2012 Igalia, S.L.
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

package org.libreplan.business.test.users.daos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.libreplan.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.libreplan.business.test.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_TEST_FILE;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.libreplan.business.common.IAdHocTransactionService;
import org.libreplan.business.common.IOnTransaction;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.resources.daos.IWorkerDAO;
import org.libreplan.business.resources.entities.Worker;
import org.libreplan.business.users.daos.IProfileDAO;
import org.libreplan.business.users.daos.IUserDAO;
import org.libreplan.business.users.entities.Profile;
import org.libreplan.business.users.entities.User;
import org.libreplan.business.users.entities.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests for <code>IUserDAO</code>.
 *
 * @author Fernando Bellas Permuy <fbellas@udc.es>
 * @author Jacobo Aragunde Perez <jaragunde@igalia.com>
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { BUSINESS_SPRING_CONFIG_FILE,
    BUSINESS_SPRING_CONFIG_TEST_FILE })
public class UserDAOTest {

    @Autowired
    private IAdHocTransactionService transactionService;

    @Autowired
    private IUserDAO userDAO;

    @Autowired
    IProfileDAO profileDAO;

    @Autowired
    private IWorkerDAO workerDAO;

    @Test
    @Transactional
    public void testBasicSave() throws InstanceNotFoundException {

        User user = createUser(getUniqueName());

        userDAO.save(user);

        User user2 = userDAO.find(user.getId());
        assertEquals(user, user2);

    }

    @Test
    public void testExistsByLoginNameAnotherTransaction() {

        final String loginName = getUniqueName();

        IOnTransaction<Void> createUser = new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                userDAO.save(createUser(loginName));
                return null;
            }
        };

        transactionService.runOnTransaction(createUser);

        assertTrue(userDAO.existsByLoginNameAnotherTransaction(loginName));
        assertTrue(userDAO.existsByLoginNameAnotherTransaction(
            loginName.toUpperCase()));
        assertTrue(userDAO.existsByLoginNameAnotherTransaction(
            loginName.toLowerCase()));

    }

    @Test
    public void testCreateWithExistingLoginName() {

        final String loginName = getUniqueName();

        IOnTransaction<Void> createUser = new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                userDAO.save(createUser(loginName));
                return null;
            }
        };

        transactionService.runOnTransaction(createUser);

        try {
            transactionService.runOnTransaction(createUser);
            fail("ValidationException expected");
        } catch (ValidationException e) {
        }

    }

    @Test
    public void testUpdateWithExistingLoginName() {

        final String loginName1 = getUniqueName();
        final String loginName2 = getUniqueName();

        IOnTransaction<Void> createUsers = new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                userDAO.save(createUser(loginName1));
                userDAO.save(createUser(loginName2));
                return null;
            }
        };

        IOnTransaction<Void> updateUser1 = new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                User user = null;
                try {
                    user = userDAO.findByLoginName(loginName1);
                } catch (InstanceNotFoundException e) {
                    fail("InstanceNotFoundException not expected");
                }
                user.setLoginName(loginName2);
                userDAO.save(user);
                return null;
            }
        };

        transactionService.runOnTransaction(createUsers);

        try {
            transactionService.runOnTransaction(updateUser1);
            fail("ValidationException expected");
        } catch (ValidationException e) {
        }

    }

    @Test
    public void testUpdateWithTheSameLoginName() {

        final String loginName1 = getUniqueName();
        final String loginName2 = getUniqueName();

        IOnTransaction<Void> createUsers = new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                userDAO.save(createUser(loginName1));
                userDAO.save(createUser(loginName2));
                return null;
            }
        };

        IOnTransaction<Void> updateUser1 = new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                User user = null;
                try {
                    user = userDAO.findByLoginName(loginName1);
                } catch (InstanceNotFoundException e) {
                    fail("InstanceNotFoundException not expected");
                }
                user.getRoles().add(getSecondUserRole());
                userDAO.save(user);
                return null;
            }
        };

        transactionService.runOnTransaction(createUsers);
        transactionService.runOnTransaction(updateUser1);
    }

    @Test
    @Transactional
    public void testFindByName() throws InstanceNotFoundException {
        User user = createUser(getUniqueName());
        user.setDisabled(true);
        userDAO.save(user);

        assertEquals(userDAO.findByLoginName(user.getLoginName()),user);
        try {
            userDAO.findByLoginNameNotDisabled(user.getLoginName());
            fail("InstanceNotFoundException was expected");
        }
        catch(InstanceNotFoundException e) {
            assertEquals((String)e.getKey(),user.getLoginName());
        }
    }

    @Test
    @Transactional
    public void testListNotDisabled() {
        User user1 = createUser(getUniqueName());
        user1.setDisabled(true);
        userDAO.save(user1);
        User user2 = createUser(getUniqueName());
        user2.setDisabled(false);
        userDAO.save(user2);

        List<User> list = userDAO.listNotDisabled();
        assertTrue(list.contains(user2));
        assertFalse(list.contains(user1));
    }

    @Test
    @Transactional
    public void testListProfiles() throws InstanceNotFoundException{

        User user = createUser(getUniqueName());
        userDAO.save(user);

        Profile profile = createProfile(getUniqueName());
        profileDAO.save(profile);

        int previous = user.getProfiles().size();
        user.addProfile(profile);
        userDAO.save(user);
        assertEquals(previous + 1, userDAO.find(user.getId()).getProfiles().size());

        previous = user.getProfiles().size();
        user.removeProfile(profile);
        userDAO.save(user);
        assertEquals(previous - 1, userDAO.find(user.getId()).getProfiles().size());
    }

    private String getUniqueName() {
        return UUID.randomUUID().toString();
    }

    private User createUser(String loginName) {

        Set<UserRole> roles = new HashSet<UserRole>();
        roles.add(getFirstUserRole());

        return User.create(loginName, loginName, roles);

    }

    private UserRole getFirstUserRole() {
        return UserRole.values()[0];
    }

    private UserRole getSecondUserRole() {
        return UserRole.values().length == 1 ? UserRole.values()[0] :
            UserRole.values()[1];
    }

    private Profile createProfile(String profileName) {
        Set<UserRole> roles = new HashSet<UserRole>();
        return Profile.create(profileName, roles);
    }

    @Test
    @Transactional
    public void testUnoundUsers1() {
        int previous = transactionService
                .runOnAnotherReadOnlyTransaction(new IOnTransaction<Integer>() {
                    @Override
                    public Integer execute() {
                        return userDAO.getUnboundUsers(null).size();
                    }
                });

        transactionService.runOnAnotherTransaction(new IOnTransaction<Void>() {

            @Override
            public Void execute() {
                userDAO.save(createUser(getUniqueName()));
                return null;
            }
        });

        int size = transactionService
                .runOnAnotherReadOnlyTransaction(new IOnTransaction<Integer>() {
                    @Override
                    public Integer execute() {
                        return userDAO.getUnboundUsers(null).size();
                    }
                });

        assertEquals(previous + 1, size);
    }

    private Worker givenStoredWorkerRelatedTo(final User user) {
        return transactionService
                .runOnAnotherTransaction(new IOnTransaction<Worker>() {

                    @Override
                    public Worker execute() {
                        Worker worker = Worker.create();
                        worker.setFirstName("Name " + UUID.randomUUID());
                        worker.setSurname("Surname " + UUID.randomUUID());
                        worker.setNif("ID " + UUID.randomUUID());
                        worker.setUser(user);
                        workerDAO.save(worker);
                        worker.dontPoseAsTransientObjectAnymore();

                        return worker;
                    }
                });
    }

    @Test
    @Transactional
    public void testUnoundUsers2() {
        int previous = transactionService
                .runOnAnotherReadOnlyTransaction(new IOnTransaction<Integer>() {
                    @Override
                    public Integer execute() {
                        return userDAO.getUnboundUsers(null).size();
                    }
                });

        User user = createUser(getUniqueName());
        user.setWorker(givenStoredWorkerRelatedTo(user));

        int size = transactionService
                .runOnAnotherReadOnlyTransaction(new IOnTransaction<Integer>() {
                    @Override
                    public Integer execute() {
                        return userDAO.getUnboundUsers(null).size();
                    }
                });

        assertEquals(previous, size);
    }

    @Test
    @Transactional
    public void testUnoundUsers3() {
        int previous = transactionService
                .runOnAnotherReadOnlyTransaction(new IOnTransaction<Integer>() {
                    @Override
                    public Integer execute() {
                        return userDAO.getUnboundUsers(null).size();
                    }
                });

        User user = createUser(getUniqueName());
        final Worker worker = givenStoredWorkerRelatedTo(user);
        user.setWorker(worker);

        int size = transactionService
                .runOnAnotherReadOnlyTransaction(new IOnTransaction<Integer>() {
                    @Override
                    public Integer execute() {
                        return userDAO.getUnboundUsers(worker).size();
                    }
                });
        assertEquals(previous + 1, size);
    }

}
