//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2017.09.18 at 02:35:45 PM BST
//

package net.sf.mpxj.primavera.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for UpdateBaselineOptionType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="UpdateBaselineOptionType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ActivitiesFilter" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ActivitiesFilterLogic" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;maxLength value="255"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="ActivityCodeAssignments" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="ActivityFilterId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ActivityFilterName" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;maxLength value="255"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="ActivityInformation" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="ActivityNotebooks" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="ActivityRsrcAssignmentUdfs" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="ActivityUdfs" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="ActualUnitsCostWoRsrcAssignmnt" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="AddNewActivitiesData" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="AddNewRsrcRole" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="AllActivities" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="BatchModeEnabled" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="BudgetUnitsCost" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="BudgetUnitsCostWoRsrcAssignmnt" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="Constraints" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="DatesDurationDatadates" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="DeleteNonExistingActivities" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="ExpenseUdfs" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="Expenses" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="FilteredActivities" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="GeneralActivitiInfo" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="IgnoreLastUpdateDate" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="IssueUDFs" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="ObjectId" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="ProjectDetails" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="ProjectRisksIssuesAndThresholds" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="ProjectUDFs" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="Relationships" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="RiskAssignments" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="RiskUDFs" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="Steps" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="StepsUdf" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="UpdateExistRsrcRoleAssignment" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="UpdateExistingActivities" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="UserName" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;maxLength value="255"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="WPDocumentUDFs" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="WbsAssignments" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="WbsUDFs" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="WorkProductsAndDocuments" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD) @XmlType(name = "UpdateBaselineOptionType", propOrder =
{
   "activitiesFilter",
   "activitiesFilterLogic",
   "activityCodeAssignments",
   "activityFilterId",
   "activityFilterName",
   "activityInformation",
   "activityNotebooks",
   "activityRsrcAssignmentUdfs",
   "activityUdfs",
   "actualUnitsCostWoRsrcAssignmnt",
   "addNewActivitiesData",
   "addNewRsrcRole",
   "allActivities",
   "batchModeEnabled",
   "budgetUnitsCost",
   "budgetUnitsCostWoRsrcAssignmnt",
   "constraints",
   "datesDurationDatadates",
   "deleteNonExistingActivities",
   "expenseUdfs",
   "expenses",
   "filteredActivities",
   "generalActivitiInfo",
   "ignoreLastUpdateDate",
   "issueUDFs",
   "objectId",
   "projectDetails",
   "projectRisksIssuesAndThresholds",
   "projectUDFs",
   "relationships",
   "riskAssignments",
   "riskUDFs",
   "steps",
   "stepsUdf",
   "updateExistRsrcRoleAssignment",
   "updateExistingActivities",
   "userName",
   "wpDocumentUDFs",
   "wbsAssignments",
   "wbsUDFs",
   "workProductsAndDocuments"
}) public class UpdateBaselineOptionType
{

   @XmlElement(name = "ActivitiesFilter") protected String activitiesFilter;
   @XmlElement(name = "ActivitiesFilterLogic") protected String activitiesFilterLogic;
   @XmlElement(name = "ActivityCodeAssignments", nillable = true) protected Boolean activityCodeAssignments;
   @XmlElement(name = "ActivityFilterId") protected String activityFilterId;
   @XmlElement(name = "ActivityFilterName") protected String activityFilterName;
   @XmlElement(name = "ActivityInformation", nillable = true) protected Boolean activityInformation;
   @XmlElement(name = "ActivityNotebooks", nillable = true) protected Boolean activityNotebooks;
   @XmlElement(name = "ActivityRsrcAssignmentUdfs", nillable = true) protected Boolean activityRsrcAssignmentUdfs;
   @XmlElement(name = "ActivityUdfs", nillable = true) protected Boolean activityUdfs;
   @XmlElement(name = "ActualUnitsCostWoRsrcAssignmnt", nillable = true) protected Boolean actualUnitsCostWoRsrcAssignmnt;
   @XmlElement(name = "AddNewActivitiesData", nillable = true) protected Boolean addNewActivitiesData;
   @XmlElement(name = "AddNewRsrcRole", nillable = true) protected Boolean addNewRsrcRole;
   @XmlElement(name = "AllActivities", nillable = true) protected Boolean allActivities;
   @XmlElement(name = "BatchModeEnabled", nillable = true) protected Boolean batchModeEnabled;
   @XmlElement(name = "BudgetUnitsCost", nillable = true) protected Boolean budgetUnitsCost;
   @XmlElement(name = "BudgetUnitsCostWoRsrcAssignmnt", nillable = true) protected Boolean budgetUnitsCostWoRsrcAssignmnt;
   @XmlElement(name = "Constraints", nillable = true) protected Boolean constraints;
   @XmlElement(name = "DatesDurationDatadates", nillable = true) protected Boolean datesDurationDatadates;
   @XmlElement(name = "DeleteNonExistingActivities", nillable = true) protected Boolean deleteNonExistingActivities;
   @XmlElement(name = "ExpenseUdfs", nillable = true) protected Boolean expenseUdfs;
   @XmlElement(name = "Expenses", nillable = true) protected Boolean expenses;
   @XmlElement(name = "FilteredActivities", nillable = true) protected Boolean filteredActivities;
   @XmlElement(name = "GeneralActivitiInfo", nillable = true) protected Boolean generalActivitiInfo;
   @XmlElement(name = "IgnoreLastUpdateDate", nillable = true) protected Boolean ignoreLastUpdateDate;
   @XmlElement(name = "IssueUDFs", nillable = true) protected Boolean issueUDFs;
   @XmlElement(name = "ObjectId", nillable = true) protected Integer objectId;
   @XmlElement(name = "ProjectDetails", nillable = true) protected Boolean projectDetails;
   @XmlElement(name = "ProjectRisksIssuesAndThresholds", nillable = true) protected Boolean projectRisksIssuesAndThresholds;
   @XmlElement(name = "ProjectUDFs", nillable = true) protected Boolean projectUDFs;
   @XmlElement(name = "Relationships", nillable = true) protected Boolean relationships;
   @XmlElement(name = "RiskAssignments", nillable = true) protected Boolean riskAssignments;
   @XmlElement(name = "RiskUDFs", nillable = true) protected Boolean riskUDFs;
   @XmlElement(name = "Steps", nillable = true) protected Boolean steps;
   @XmlElement(name = "StepsUdf", nillable = true) protected Boolean stepsUdf;
   @XmlElement(name = "UpdateExistRsrcRoleAssignment", nillable = true) protected Boolean updateExistRsrcRoleAssignment;
   @XmlElement(name = "UpdateExistingActivities", nillable = true) protected Boolean updateExistingActivities;
   @XmlElement(name = "UserName") protected String userName;
   @XmlElement(name = "WPDocumentUDFs", nillable = true) protected Boolean wpDocumentUDFs;
   @XmlElement(name = "WbsAssignments", nillable = true) protected Boolean wbsAssignments;
   @XmlElement(name = "WbsUDFs", nillable = true) protected Boolean wbsUDFs;
   @XmlElement(name = "WorkProductsAndDocuments", nillable = true) protected Boolean workProductsAndDocuments;

   /**
    * Gets the value of the activitiesFilter property.
    *
    * @return
    *     possible object is
    *     {@link String }
    *
    */
   public String getActivitiesFilter()
   {
      return activitiesFilter;
   }

   /**
    * Sets the value of the activitiesFilter property.
    *
    * @param value
    *     allowed object is
    *     {@link String }
    *
    */
   public void setActivitiesFilter(String value)
   {
      this.activitiesFilter = value;
   }

   /**
    * Gets the value of the activitiesFilterLogic property.
    *
    * @return
    *     possible object is
    *     {@link String }
    *
    */
   public String getActivitiesFilterLogic()
   {
      return activitiesFilterLogic;
   }

   /**
    * Sets the value of the activitiesFilterLogic property.
    *
    * @param value
    *     allowed object is
    *     {@link String }
    *
    */
   public void setActivitiesFilterLogic(String value)
   {
      this.activitiesFilterLogic = value;
   }

   /**
    * Gets the value of the activityCodeAssignments property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isActivityCodeAssignments()
   {
      return activityCodeAssignments;
   }

   /**
    * Sets the value of the activityCodeAssignments property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setActivityCodeAssignments(Boolean value)
   {
      this.activityCodeAssignments = value;
   }

   /**
    * Gets the value of the activityFilterId property.
    *
    * @return
    *     possible object is
    *     {@link String }
    *
    */
   public String getActivityFilterId()
   {
      return activityFilterId;
   }

   /**
    * Sets the value of the activityFilterId property.
    *
    * @param value
    *     allowed object is
    *     {@link String }
    *
    */
   public void setActivityFilterId(String value)
   {
      this.activityFilterId = value;
   }

   /**
    * Gets the value of the activityFilterName property.
    *
    * @return
    *     possible object is
    *     {@link String }
    *
    */
   public String getActivityFilterName()
   {
      return activityFilterName;
   }

   /**
    * Sets the value of the activityFilterName property.
    *
    * @param value
    *     allowed object is
    *     {@link String }
    *
    */
   public void setActivityFilterName(String value)
   {
      this.activityFilterName = value;
   }

   /**
    * Gets the value of the activityInformation property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isActivityInformation()
   {
      return activityInformation;
   }

   /**
    * Sets the value of the activityInformation property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setActivityInformation(Boolean value)
   {
      this.activityInformation = value;
   }

   /**
    * Gets the value of the activityNotebooks property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isActivityNotebooks()
   {
      return activityNotebooks;
   }

   /**
    * Sets the value of the activityNotebooks property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setActivityNotebooks(Boolean value)
   {
      this.activityNotebooks = value;
   }

   /**
    * Gets the value of the activityRsrcAssignmentUdfs property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isActivityRsrcAssignmentUdfs()
   {
      return activityRsrcAssignmentUdfs;
   }

   /**
    * Sets the value of the activityRsrcAssignmentUdfs property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setActivityRsrcAssignmentUdfs(Boolean value)
   {
      this.activityRsrcAssignmentUdfs = value;
   }

   /**
    * Gets the value of the activityUdfs property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isActivityUdfs()
   {
      return activityUdfs;
   }

   /**
    * Sets the value of the activityUdfs property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setActivityUdfs(Boolean value)
   {
      this.activityUdfs = value;
   }

   /**
    * Gets the value of the actualUnitsCostWoRsrcAssignmnt property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isActualUnitsCostWoRsrcAssignmnt()
   {
      return actualUnitsCostWoRsrcAssignmnt;
   }

   /**
    * Sets the value of the actualUnitsCostWoRsrcAssignmnt property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setActualUnitsCostWoRsrcAssignmnt(Boolean value)
   {
      this.actualUnitsCostWoRsrcAssignmnt = value;
   }

   /**
    * Gets the value of the addNewActivitiesData property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isAddNewActivitiesData()
   {
      return addNewActivitiesData;
   }

   /**
    * Sets the value of the addNewActivitiesData property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setAddNewActivitiesData(Boolean value)
   {
      this.addNewActivitiesData = value;
   }

   /**
    * Gets the value of the addNewRsrcRole property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isAddNewRsrcRole()
   {
      return addNewRsrcRole;
   }

   /**
    * Sets the value of the addNewRsrcRole property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setAddNewRsrcRole(Boolean value)
   {
      this.addNewRsrcRole = value;
   }

   /**
    * Gets the value of the allActivities property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isAllActivities()
   {
      return allActivities;
   }

   /**
    * Sets the value of the allActivities property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setAllActivities(Boolean value)
   {
      this.allActivities = value;
   }

   /**
    * Gets the value of the batchModeEnabled property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isBatchModeEnabled()
   {
      return batchModeEnabled;
   }

   /**
    * Sets the value of the batchModeEnabled property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setBatchModeEnabled(Boolean value)
   {
      this.batchModeEnabled = value;
   }

   /**
    * Gets the value of the budgetUnitsCost property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isBudgetUnitsCost()
   {
      return budgetUnitsCost;
   }

   /**
    * Sets the value of the budgetUnitsCost property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setBudgetUnitsCost(Boolean value)
   {
      this.budgetUnitsCost = value;
   }

   /**
    * Gets the value of the budgetUnitsCostWoRsrcAssignmnt property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isBudgetUnitsCostWoRsrcAssignmnt()
   {
      return budgetUnitsCostWoRsrcAssignmnt;
   }

   /**
    * Sets the value of the budgetUnitsCostWoRsrcAssignmnt property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setBudgetUnitsCostWoRsrcAssignmnt(Boolean value)
   {
      this.budgetUnitsCostWoRsrcAssignmnt = value;
   }

   /**
    * Gets the value of the constraints property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isConstraints()
   {
      return constraints;
   }

   /**
    * Sets the value of the constraints property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setConstraints(Boolean value)
   {
      this.constraints = value;
   }

   /**
    * Gets the value of the datesDurationDatadates property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isDatesDurationDatadates()
   {
      return datesDurationDatadates;
   }

   /**
    * Sets the value of the datesDurationDatadates property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setDatesDurationDatadates(Boolean value)
   {
      this.datesDurationDatadates = value;
   }

   /**
    * Gets the value of the deleteNonExistingActivities property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isDeleteNonExistingActivities()
   {
      return deleteNonExistingActivities;
   }

   /**
    * Sets the value of the deleteNonExistingActivities property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setDeleteNonExistingActivities(Boolean value)
   {
      this.deleteNonExistingActivities = value;
   }

   /**
    * Gets the value of the expenseUdfs property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isExpenseUdfs()
   {
      return expenseUdfs;
   }

   /**
    * Sets the value of the expenseUdfs property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setExpenseUdfs(Boolean value)
   {
      this.expenseUdfs = value;
   }

   /**
    * Gets the value of the expenses property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isExpenses()
   {
      return expenses;
   }

   /**
    * Sets the value of the expenses property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setExpenses(Boolean value)
   {
      this.expenses = value;
   }

   /**
    * Gets the value of the filteredActivities property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isFilteredActivities()
   {
      return filteredActivities;
   }

   /**
    * Sets the value of the filteredActivities property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setFilteredActivities(Boolean value)
   {
      this.filteredActivities = value;
   }

   /**
    * Gets the value of the generalActivitiInfo property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isGeneralActivitiInfo()
   {
      return generalActivitiInfo;
   }

   /**
    * Sets the value of the generalActivitiInfo property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setGeneralActivitiInfo(Boolean value)
   {
      this.generalActivitiInfo = value;
   }

   /**
    * Gets the value of the ignoreLastUpdateDate property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isIgnoreLastUpdateDate()
   {
      return ignoreLastUpdateDate;
   }

   /**
    * Sets the value of the ignoreLastUpdateDate property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setIgnoreLastUpdateDate(Boolean value)
   {
      this.ignoreLastUpdateDate = value;
   }

   /**
    * Gets the value of the issueUDFs property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isIssueUDFs()
   {
      return issueUDFs;
   }

   /**
    * Sets the value of the issueUDFs property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setIssueUDFs(Boolean value)
   {
      this.issueUDFs = value;
   }

   /**
    * Gets the value of the objectId property.
    *
    * @return
    *     possible object is
    *     {@link Integer }
    *
    */
   public Integer getObjectId()
   {
      return objectId;
   }

   /**
    * Sets the value of the objectId property.
    *
    * @param value
    *     allowed object is
    *     {@link Integer }
    *
    */
   public void setObjectId(Integer value)
   {
      this.objectId = value;
   }

   /**
    * Gets the value of the projectDetails property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isProjectDetails()
   {
      return projectDetails;
   }

   /**
    * Sets the value of the projectDetails property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setProjectDetails(Boolean value)
   {
      this.projectDetails = value;
   }

   /**
    * Gets the value of the projectRisksIssuesAndThresholds property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isProjectRisksIssuesAndThresholds()
   {
      return projectRisksIssuesAndThresholds;
   }

   /**
    * Sets the value of the projectRisksIssuesAndThresholds property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setProjectRisksIssuesAndThresholds(Boolean value)
   {
      this.projectRisksIssuesAndThresholds = value;
   }

   /**
    * Gets the value of the projectUDFs property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isProjectUDFs()
   {
      return projectUDFs;
   }

   /**
    * Sets the value of the projectUDFs property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setProjectUDFs(Boolean value)
   {
      this.projectUDFs = value;
   }

   /**
    * Gets the value of the relationships property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isRelationships()
   {
      return relationships;
   }

   /**
    * Sets the value of the relationships property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setRelationships(Boolean value)
   {
      this.relationships = value;
   }

   /**
    * Gets the value of the riskAssignments property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isRiskAssignments()
   {
      return riskAssignments;
   }

   /**
    * Sets the value of the riskAssignments property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setRiskAssignments(Boolean value)
   {
      this.riskAssignments = value;
   }

   /**
    * Gets the value of the riskUDFs property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isRiskUDFs()
   {
      return riskUDFs;
   }

   /**
    * Sets the value of the riskUDFs property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setRiskUDFs(Boolean value)
   {
      this.riskUDFs = value;
   }

   /**
    * Gets the value of the steps property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isSteps()
   {
      return steps;
   }

   /**
    * Sets the value of the steps property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setSteps(Boolean value)
   {
      this.steps = value;
   }

   /**
    * Gets the value of the stepsUdf property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isStepsUdf()
   {
      return stepsUdf;
   }

   /**
    * Sets the value of the stepsUdf property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setStepsUdf(Boolean value)
   {
      this.stepsUdf = value;
   }

   /**
    * Gets the value of the updateExistRsrcRoleAssignment property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isUpdateExistRsrcRoleAssignment()
   {
      return updateExistRsrcRoleAssignment;
   }

   /**
    * Sets the value of the updateExistRsrcRoleAssignment property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setUpdateExistRsrcRoleAssignment(Boolean value)
   {
      this.updateExistRsrcRoleAssignment = value;
   }

   /**
    * Gets the value of the updateExistingActivities property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isUpdateExistingActivities()
   {
      return updateExistingActivities;
   }

   /**
    * Sets the value of the updateExistingActivities property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setUpdateExistingActivities(Boolean value)
   {
      this.updateExistingActivities = value;
   }

   /**
    * Gets the value of the userName property.
    *
    * @return
    *     possible object is
    *     {@link String }
    *
    */
   public String getUserName()
   {
      return userName;
   }

   /**
    * Sets the value of the userName property.
    *
    * @param value
    *     allowed object is
    *     {@link String }
    *
    */
   public void setUserName(String value)
   {
      this.userName = value;
   }

   /**
    * Gets the value of the wpDocumentUDFs property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isWPDocumentUDFs()
   {
      return wpDocumentUDFs;
   }

   /**
    * Sets the value of the wpDocumentUDFs property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setWPDocumentUDFs(Boolean value)
   {
      this.wpDocumentUDFs = value;
   }

   /**
    * Gets the value of the wbsAssignments property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isWbsAssignments()
   {
      return wbsAssignments;
   }

   /**
    * Sets the value of the wbsAssignments property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setWbsAssignments(Boolean value)
   {
      this.wbsAssignments = value;
   }

   /**
    * Gets the value of the wbsUDFs property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isWbsUDFs()
   {
      return wbsUDFs;
   }

   /**
    * Sets the value of the wbsUDFs property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setWbsUDFs(Boolean value)
   {
      this.wbsUDFs = value;
   }

   /**
    * Gets the value of the workProductsAndDocuments property.
    *
    * @return
    *     possible object is
    *     {@link Boolean }
    *
    */
   public Boolean isWorkProductsAndDocuments()
   {
      return workProductsAndDocuments;
   }

   /**
    * Sets the value of the workProductsAndDocuments property.
    *
    * @param value
    *     allowed object is
    *     {@link Boolean }
    *
    */
   public void setWorkProductsAndDocuments(Boolean value)
   {
      this.workProductsAndDocuments = value;
   }

}
