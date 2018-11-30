How To Create A New Report In LibrePlan
=======================================

.. sectnum::

:Author: Manuel Rego Casasnovas
:Contact: mrego@igalia.com
:Date: 11/09/2012
:Copyright:
      Some rights reserved. This document is distributed under the Creative
      Commons Attribution-ShareAlike 3.0 licence, available in
      http://creativecommons.org/licenses/by-sa/3.0/.
:Abstract:
      LibrePlan uses **JasperReports** [1]_ to create reports in the application.
  This document tries to explain how to create a new report in LibrePlan.

  During this tutorial you are going to create a report that will show the list of resources in LibrePlan.

.. contents:: Table of Contents

Add an entry on LibrePlan menu
------------------------------

First of all, you are going to add a new entry on *Reports* menu in LibrePlan,
this option will link to a new ``.zul`` file inside
``libreplan-webapp/src/main/webapp/reports/`` that will be the basic
interface for users before generate the report.

Steps:

* Modify method ``initializeMenu()`` in ``CustomMenuController.java`` to add a
  new ``subItem`` inside the ``topItem`` *Reports*::

    subItem(_("Resources List"),
        "/reports/resourcesListReport.zul",
        "15-informes.html")

You will see the new entry if you run LibrePlan, but the link is not going to
work as ``.zul`` page still does not exist.

.. TIP::

   If you want to run LibrePlan in development mode you need to follow the next
   instructions:

   * Create a PostgreSQL database called ``libreplandev`` with permissions for a
     user ``libreplan`` with    password ``libreplan`` (see ``INSTALL`` file for
     other databases and more info).

   * Compile LibrePlan with the following command from project root folder::

       mvn -DskipTests -P-userguide clean install

   * Launch Jetty from ``libreplan-webapp`` directory::

       cd libreplan-webapp
       mvn -P-userguide jetty:run

   * Access with a web browser to the following URL and login with default
     credentials (user ``admin`` and password ``admin``):
     http://localhost:8080/libreplan-webapp/


Create basic HTML interface
---------------------------

You need an interface were users could specify some parameters (if needed) for
the report and then generate the expected result. This interface will be
linked from the menu entry added before. For the moment, you are going to create
a very basic interface, copying some parts from other reports.

Steps:

* Create a new file ``resourcesListReport.zul`` in
  ``libreplan-webapp/src/main/webapp/reports/``. With the following content:

::

 <!--
     This file is part of LibrePlan

     Copyright (C) 2011 Igalia

     This program is free software: you can redistribute it and/or modify
     it under the terms of the GNU Affero General Public License as published by
     the Free Software Foundation, either version 3 of the License, or
     (at your option) any later version.

     This program is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU Affero General Public License for more details.

     You should have received a copy of the GNU Affero General Public License
     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 -->

 <?page title="${i18n:_('LibrePlan: Resources List')}" id="reports"?>
 <?init class="org.zkoss.zkplus.databind.AnnotateDataBinderInit" ?>
 <?init class="org.zkoss.zk.ui.util.Composition" arg0="/common/layout/template.zul"?>

 <?link rel="stylesheet" type="text/css" href="/common/css/libreplan.css"?>
 <?link rel="stylesheet" type="text/css" href="/common/css/libreplan_zk.css"?>

 <?variable-resolver class="org.zkoss.zkplus.spring.DelegatingVariableResolver"?>

 <?component name="comboboxOutputFormat" macroURI="comboboxOutputFormat.zul"
     class="org.libreplan.web.reports.ComboboxOutputFormat" ?>

 <zk>

     <window self="@{define(content)}"
         apply="org.libreplan.web.reports.ResourcesListReportController"
         title="${i18n:_('Resources List')}"
         border="normal" >

         <!-- Select output format -->
         <panel title="${i18n:_('Format')}" border="normal"
             style="overflow:auto">
             <panelchildren>
                 <grid width="700px">
                     <columns>
                         <column width="200px" />
                         <column />
                     </columns>
                     <rows>
                         <row>
                             <label value="${i18n:_('Output format:')}" />
                             <comboboxOutputFormat id="outputFormat" />
                         </row>
                     </rows>
                 </grid>
             </panelchildren>
         </panel>

         <separator spacing="10px" orient="horizontal" />

         <hbox style="display: none" id="URItext">
             <label value="${i18n:_('Click on this')}" />
             <a id="URIlink" class="z-label" zclass="z-label"
                 label="${i18n:_('direct link')}" />
             <label
                 value="${i18n:_('if the report is not opened automatically')}" />
         </hbox>

         <separator spacing="10px" orient="horizontal" />

         <button label="Show" onClick="controller.showReport(report)" />

         <jasperreportcomponent id="report" />

     </window>

 </zk>

This will create a basic interface for report with a combo to select the desired
output format for it and a button to generate the report. As we can see it uses
``ResourcesListReportController`` that will be created in the next point.


Create a controller for new report
----------------------------------

As you can see previous ``.zul`` file defined uses a controller that will be in
charge to manage users interaction with report interface and call the proper
methods to generate the report itself and show it to the user.

There is already a controller called ``LibrePlanReportController`` which
implements most of the stuff needed for report controllers.
So, controllers for new reports are going to extend this class and re-implement some methods.

Steps:

* Create a new file ``ResourcesListReportController.java`` in
  ``libreplan-webapp/src/main/java/org/libreplan/web/reports/`` with the
  following content:

::

 /*
  * This file is part of LibrePlan
  *
  * Copyright (C) 2011 Igalia, S.L.
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

 package org.libreplan.web.reports;

 import net.sf.jasperreports.engine.JRDataSource;
 import net.sf.jasperreports.engine.JREmptyDataSource;

 import org.zkoss.zk.ui.Component;

 /**
  * Controller for UI operations of Resources List report.
  *
  * @author Manuel Rego Casasnovas <mrego@igalia.com>
  */
 public class ResourcesListReportController extends LibrePlanReportController {

     private static final String REPORT_NAME = "resourcesListReport";

     @Override
     public void doAfterCompose(Component comp) throws Exception {
         super.doAfterCompose(comp);
         comp.setAttribute("controller", this);
     }

     @Override
     protected String getReportName() {
         return REPORT_NAME;
     }

     @Override
     protected JRDataSource getDataSource() {
         return new JREmptyDataSource();
     }

 }

Now if you run LibrePlan and access to the new menu entry you will see the
simple form allowing you to choose the output format for the report and also the
button to show it (that will not work yet).


Create a DTO
------------

As usually reports show information extracted from database but with some
specific modifications, for example, merging data from different database
tables; you will need to define a DTO (Data Transfer Object) with the fields
that you want to show in the report.

In your case the DTO is pretty simple, you will show for each resource: code and
name.

Steps:

* Create a new file ``ResourcesListReportDTO.java`` in
  ``libreplan-business/src/main/java/org/libreplan/business/reports/dtos/``
  with the following content:

::

 /*
  * This file is part of LibrePlan
  *
  * Copyright (C) 2011 Igalia, S.L.
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

 package org.libreplan.business.reports.dtos;

 /**
  * DTO for Resources List report data.
  *
  * @author Manuel Rego Casasnovas <mrego@igalia.com>
  */
 public class ResourcesListReportDTO {

     private String code;

     private String name;

     public ResourcesListReportDTO(String code, String name) {
         this.code = code;
         this.name = name;
     }

     public String getCode() {
         return code;
     }

     public String getName() {
         return name;
     }

 }

A list of DTOs will be passed to JasperReports in order to generate the report
with the data.


Define report layout (iReport)
------------------------------

Now that you know which data you are going to show in the report (check DTOs
attributes) you should define the JasperReports format with a XML.

You need to install **iReport** [2]_, it is a tool used to define and design
report layouts, which provides a visual interface to define ``.jrxml`` file.

Steps:

* Download iReport **4.7.0** (``tar.gz``) from SourceForge.net:
  https://sourceforge.net/projects/ireport/files/iReport/

* Uncompress file::

    tar -xvzf iReport-4.7.0.tar.gz

* Launch iReport::

    cd iReport-4.7.0/
    ./bin/ireport

* Open some existent LibrePlan report (e.g.
  ``hoursWorkedPerWorkerInAMonthReport.jrxml``) under
  ``libreplan-webapp/src/main/jasper`` to use as template to keep the same
  layout and save it with the name of the new report
  ``resourcesListReport.jrxml`` in the same folder.

  This will allow us to keep coherence between reports in regard to design, header, footer, etc.

* Set report name to ``resourcesList``.

* Set resource bundle to ``resourcesList``.

* Remove following parameters:

  * ``startingDate``
  * ``endingDate``
  * ``showNote``

* Remove all the fields and add the following:

  * Name: ``code``, class: ``java.lang.String``
  * Name: ``name``, class: ``java.lang.String``

* Remove following variables:

  * ``sumHoursPerDay``
  * ``sumHoursPerWorker``

* Remove following elements in *Title* band:

  * ``$R{date.start}``
  * ``$R{date.end}``
  * ``$P{startingDate}``
  * ``$P{endingDate}``
  * ``$R{note1}``
  * Label: ``*``

* Set ``Band height`` in *Title* band to ``80``.

* Remove group *Worker group Group Header 1*.

* Remove group *Date group Group Header 1*.

* Remove columns in *Detail 1* band in order to leave only 2 columns:
  ``$F{code}`` and ``$F{name}``.

Now you have defined a very basic report layout using some common elements
with other LibrePlan reports like header and footer. The result in iReport would
be something similar to the screenshot.

.. figure:: img/ireport-resources-list-report.png
:alt: iRerpot screenshot for Resources List report
   :width: 100%

       iReport screenshot for Resources List report

    You can even check the XML ``resourcesListReport.jrxml`` that should have
something similar to the following content:

::

 <?xml version="1.0" encoding="UTF-8"?>
 <jasperReport xmlns="http://jasperreports.sourceforge.net/jasperreports"
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 xsi:schemaLocation="http://jasperreports.sourceforge.net/jasperreports
 http://jasperreports.sourceforge.net/xsd/jasperreport.xsd" name="resourcesList" pageWidth="595" pageHeight="842"
 columnWidth="535" leftMargin="20" rightMargin="20" topMargin="20" bottomMargin="20"
 resourceBundle="resourcesList" uuid="f83422af-00de-4fa5-b137-580b559f1453">

     <property name="ireport.zoom" value="1.0"/>
     <property name="ireport.x" value="0"/>
     <property name="ireport.y" value="0"/>
     <style name="dejavu-sans" isDefault="true" fontName="DejaVu Sans" fontSize="8"/>
     <parameter name="logo" class="java.lang.String"/>
     <field name="code" class="java.lang.String"/>
     <field name="name" class="java.lang.String"/>
     <background>
         <band splitType="Stretch"/>
     </background>
     <title>
         <band height="80" splitType="Stretch">
             <textField>
                 <reportElement uuid="6d64d335-2ffd-45e8-8915-3191baa4e278" x="0" y="13" width="263" height="33"/>
                 <textElement verticalAlignment="Middle" markup="none">
                     <font size="23" isBold="true"/>
                 </textElement>
                 <textFieldExpression><![CDATA[$R{title}]]></textFieldExpression>
             </textField>
             <textField>
                 <reportElement uuid="2174417c-89a2-4012-8915-8f8e3fa8119e" x="23" y="46" width="295" height="22"/>
                 <textElement markup="none">
                     <font size="15" isItalic="true"/>
                 </textElement>
                 <textFieldExpression><![CDATA[$R{subtitle}]]></textFieldExpression>
             </textField>
             <image scaleImage="RetainShape" isLazy="true">
                 <reportElement uuid="e033fa20-c68f-4716-9b43-e1435be185a8" x="318" y="0" width="180" height="53"/>
                 <imageExpression><![CDATA[$P{logo}]]></imageExpression>
             </image>
         </band>
     </title>
     <pageHeader>
         <band splitType="Stretch"/>
     </pageHeader>
     <columnHeader>
         <band splitType="Stretch"/>
     </columnHeader>
     <detail>
         <band height="15" splitType="Stretch">
             <textField isBlankWhenNull="true">
                 <reportElement uuid="5a829e90-0860-48dd-aeb0-262599571b4a" x="145" y="0" width="414" height="15"/>
                 <textElement textAlignment="Center" verticalAlignment="Middle"/>
                 <textFieldExpression><![CDATA[$F{name}]]></textFieldExpression>
             </textField>
             <textField isBlankWhenNull="true">
                 <reportElement uuid="78755bf1-f99a-4aa3-a87a-13ed4e54ce60" x="13" y="0" width="132" height="15"/>
                 <textElement textAlignment="Center" verticalAlignment="Middle"/>
                 <textFieldExpression><![CDATA[$F{code}]]></textFieldExpression>
             </textField>
         </band>
     </detail>
     <columnFooter>
         <band height="17" splitType="Stretch"/>
     </columnFooter>
     <pageFooter>
         <band height="27" splitType="Stretch">
             <textField pattern="EEEEE, dd MMMMM yyyy">
                 <reportElement uuid="74fb7d79-5caa-42db-b9c8-8b0e5a6b38da" x="0" y="0" width="197" height="20"/>
                 <textElement/>
                 <textFieldExpression><![CDATA[new java.util.Date()]]></textFieldExpression>
             </textField>
             <textField>
                 <reportElement uuid="1bb28642-13dd-469d-937b-0eb361cea34e" x="435" y="2" width="43" height="20"/>
                 <textElement/>
                 <textFieldExpression><![CDATA[$R{page}]]></textFieldExpression>
             </textField>
             <textField>
                 <reportElement uuid="a0067519-9437-4549-b9ca-70bb8abd86ef" x="498" y="2" width="15" height="20"/>
                 <textElement/>
                 <textFieldExpression><![CDATA[$R{of}]]></textFieldExpression>
             </textField>
             <textField evaluationTime="Report">
                 <reportElement uuid="49abaabf-9c24-4078-8551-632c03e5aebb" x="515" y="2" width="38" height="20"/>
                 <textElement/>
                 <textFieldExpression><![CDATA[$V{PAGE_NUMBER}]]></textFieldExpression>
             </textField>
             <textField>
                 <reportElement uuid="3b2e8d7e-d96f-4f30-aa3b-80300629dda7" x="478" y="2" width="15" height="20"/>
                 <textElement textAlignment="Right"/>
                 <textFieldExpression><![CDATA[$V{PAGE_NUMBER}]]></textFieldExpression>
             </textField>
         </band>
     </pageFooter>
     <summary>
         <band splitType="Stretch"/>
     </summary>
 </jasperReport>


Add report bundle for translation strings
-----------------------------------------

Once defined the report format with *iReport* you need to create an special
directory to put there translation files related with report strings.

Steps:

* Create directory called ``resourcesList_Bundle`` in
  ``libreplan-webapp/src/main/jasper/``::

    mkdir libreplan-webapp/src/main/jasper/resourcesList_Bundle

  You can check bundle folders of other reports in the same directory to see
  more   examples, but it basically contains the properties files with different
  translations for the project.

* Create a file called ``resourcesList.properties`` inside the new directory
  with the following content:

::

 # Locale for resourcesListReport.jrxml
 title = Resources List Report
 subtitle = List of resources
 page = page
 of = of

* Add the following lines in main ``pom.xml`` file at project root folder,
  in ``Report bundle directories`` section::

    <resource>
        <directory>../libreplan-webapp/src/main/jasper/resourcesList_Bundle/</directory>
    </resource>

Now jun can run LibrePlan and see the report already working, but as you are not
sending it any data (currently you are using ``JREmptyDataSource``) the report
will appear empty but you can see header with title and footer.


Create some example data
------------------------

At that point you have everything ready to generate your first report, but you
need to show some data in the report. So, you are going to add some example data
manually created to see the final result.

Steps:

* Modify ``getDataSource`` method in ``ResourcesListReportController`` created
  before and use the following content as example:

::

     @Override
     protected JRDataSource getDataSource() {
         // Example data
         ResourcesListReportDTO resource1 = new ResourcesListReportDTO("1",
                 "Jonh Doe");
         ResourcesListReportDTO resource2 = new ResourcesListReportDTO("2",
                 "Richard Roe");

         List<ResourcesListReportDTO> resourcesListDTOs = Arrays.asList(
                 resource1, resource2);

         return new JRBeanCollectionDataSource(resourcesListDTOs);
     }

Then if you run LibrePlan and go to the new menu entry called *Resources List*
in *Reports* you will be able to generate a report with the resources added as example data.
The report still lacks a good design and format, but at least you
are able to see how the basic functionality of JasperReports in LibrePlan is integrated.
The next step will be to show real data in the report getting it from database.

.. figure:: img/resources-list-report-example-data-pdf.png
:alt: Resources List report with example data in PDF format
   :width: 100%

       Resources List report with example data in PDF format


Show real data from database
----------------------------

Now you need to query database and get information about resources.
In order to follow LibrePlan architecture you are going to create a model that will be in
charge to retrieve information from database, process it if needed and return the information to the controller.
Then controller will send this information to JasperReports in order to generate the report with real data.

Steps:

* Modify ``ResourcesListReportDTO`` constructor to receive a real ``Resource``
  entity and get get information from it::

    public ResourcesListReportDTO(Resource resource) {
        this.code = resource.getCode();
        this.name = resource.getName();
    }

* Create a file ``IResourcesListReportModel.java`` in
  ``libreplan-webapp/src/main/java/org/libreplan/web/reports/`` with the
  following content:

::

 /*
  * This file is part of LibrePlan
  *
  * Copyright (C) 2011 Igalia, S.L.
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

 package org.libreplan.web.reports;

 import java.util.List;

 import org.libreplan.business.reports.dtos.ResourcesListReportDTO;

 /**
  * Interface for {@link ResourcesListReportModel}.
  *
  * @author Manuel Rego Casasnovas <mrego@igalia.com>
  */
 public interface IResourcesListReportModel {

     List<ResourcesListReportDTO> getResourcesListReportDTOs();

 }

* Create another file ``ResourcesListReportModel.java`` in the same directory
  with the following content:

::

 /*
  * This file is part of LibrePlan
  *
  * Copyright (C) 2011 Igalia, S.L.
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

 package org.libreplan.web.reports;

 import java.util.ArrayList;
 import java.util.List;

 import org.libreplan.business.reports.dtos.ResourcesListReportDTO;
 import org.libreplan.business.resources.daos.IResourceDAO;
 import org.libreplan.business.resources.entities.Resource;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.beans.factory.config.BeanDefinition;
 import org.springframework.context.annotation.Scope;
 import org.springframework.stereotype.Service;
 import org.springframework.transaction.annotation.Transactional;

 /**
  * Model for Resources List report.
  *
  * @author Manuel Rego Casasnovas <mrego@igalia.com>
  */
 @Service
 @Scope(BeanDefinition.SCOPE_PROTOTYPE)
 public class ResourcesListReportModel implements IResourcesListReportModel {

     @Autowired
     private IResourceDAO resourceDAO;

     @Override
     @Transactional(readOnly = true)
     public List<ResourcesListReportDTO> getResourcesListReportDTOs() {
         List<ResourcesListReportDTO> dtos = new ArrayList<ResourcesListReportDTO>();

         for (Resource resource : resourceDAO.getResources()) {
             dtos.add(new ResourcesListReportDTO(resource));
         }

         return dtos;
     }

 }

* Add the following line in ``ResourcesListReportController``::

    private IResourcesListReportModel resourcesListReportModel;

* Modify ``getDataSource`` method in ``ResourcesListReportController`` to use
  the model to get data from database::

    @Override
    protected JRDataSource getDataSource() {
        List<ResourcesListReportDTO> dtos = resourcesListReportModel
                .getResourcesListReportDTOs();
        if (dtos.isEmpty()) {
            return new JREmptyDataSource();
        }

        return new JRBeanCollectionDataSource(dtos);
    }

At this moment, you are going to be able to generate report with the list of all
resources currently stored in LibrePlan database.


Filter information on report
----------------------------

You are going to add a simple filter in interface to allow users to select what
kind of resources are going to appear in the report: workers or machines.

Steps:

* Modify ``resourcesListReport.zul`` to add the following lines::

     <!-- Select type of resource -->
     <panel title="${i18n:_('Type of resource')}" border="normal"
         style="overflow:auto">
         <panelchildren>
             <grid width="700px">
                 <columns>
                     <column width="200px" />
                     <column />
                 </columns>
                 <rows>
                     <row>
                         <label value="${i18n:_('Type:')}" />
                         <combobox id="resourcesType" autocomplete="true"
                            autodrop="true" value="${i18n:_('All')}">
                            <comboitem label="${i18n:_('All')}"
                                value="all" />
                            <comboitem label="${i18n:_('Workers')}"
                                value="workers" />
                            <comboitem label="${i18n:_('Machines')}"
                                value="machines" />
                        </combobox>
                     </row>
                 </rows>
             </grid>
         </panelchildren>
     </panel>

* Add following line in ``ResourcesListReportController``::

    private Combobox resourcesType;

* And modify ``getDataSource`` method in the same file::

    @Override
    protected JRDataSource getDataSource() {
        Comboitem typeSelected = resourcesType.getSelectedItemApi();
        String type = (typeSelected == null) ? "all" : (String) typeSelected
                .getValue();

        List<ResourcesListReportDTO> dtos = resourcesListReportModel
                .getResourcesListReportDTOs(type);
        if (dtos.isEmpty()) {
            return new JREmptyDataSource();
        }

        return new JRBeanCollectionDataSource(dtos);
    }

* This would mean that a new parameter appear in model method, so you would need
  to modify ``IResourcesListReportModel`` to add the new parameter ::

    List<ResourcesListReportDTO> getResourcesListReportDTOs(String type);

  And change ``getResourcesListReportDTOs`` method in
  ``ResourcesListReportModel`` to get different information depending on the new
  parameter::

    @Override
    @Transactional(readOnly = true)
    public List<ResourcesListReportDTO> getResourcesListReportDTOs(String type) {
        List<ResourcesListReportDTO> dtos = new ArrayList<ResourcesListReportDTO>();

        List<? extends Resource> resources;
        if (type.equals("workers")) {
            resources = resourceDAO.getWorkers();
        } else if (type.equals("machines")) {
            resources = resourceDAO.getMachines();
        } else {
            resources = resourceDAO.getResources();
        }

        for (Resource resource : resources) {
            dtos.add(new ResourcesListReportDTO(resource));
        }

        return dtos;
    }

After applying these changes you will be able to filter the report depending on
option selected by users in the interface.


Send parameters to report
-------------------------

Sometimes you need to send parameters to be printed in the report. You are
already doing it without noticing, for example, you are sending logo path. You
can check ``getParameters`` method in ``LibrePlanReportController``.

Now you are going to send a parameter to print a message specifying if you are
printing all the resources or just workers or machines using the filter.

Steps:

* Override ``getParameters`` in ``ResourcesListReportController`` using the
  following lines::

    @Override
    protected Map<String, Object> getParameters() {
        Map<String, Object> result = super.getParameters();

        result.put("type", resourcesType.getValue());
        return result;
    }

* Modify report file ``resourcesListReport.jrxml`` with iReport to add the new
  parameter and show it in some part of the report layout.
  You could use iReport for this task, or, for example, add the following lines in XML file::

    <parameter name="type" class="java.lang.String"/>

    ...

    <columnHeader>
        <band height="25" splitType="Stretch">
            <textField>
                <reportElement x="0" y="0" width="58" height="18"/>
                <textElement verticalAlignment="Middle" markup="none">
                    <font size="10" isBold="true"/>
                </textElement>
                <textFieldExpression class="java.lang.String"><![CDATA[$R{type}]]></textFieldExpression>
            </textField>
            <textField>
                <reportElement x="58" y="0" width="328" height="18"/>
                <textElement verticalAlignment="Middle" markup="none">
                    <font size="10" isBold="false"/>
                </textElement>
                <textFieldExpression class="java.lang.String"><![CDATA[$P{type}]]></textFieldExpression>
            </textField>
        </band>
    </columnHeader>

  It is also needed to add the new label in ``.properties`` file::

    type = Type:

Now if you generate the report you will see the type of report you are
generating, you can see more examples about how to send parameters in some of
the other reports already implemented in LibrePlan.


.. [1] http://jasperforge.org/jasperreports
.. [2] http://jasperforge.org/projects/ireport
