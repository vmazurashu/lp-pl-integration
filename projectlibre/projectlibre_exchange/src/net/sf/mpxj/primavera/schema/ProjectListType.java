//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2017.09.18 at 02:35:45 PM BST
//

package net.sf.mpxj.primavera.schema;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for ProjectListType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ProjectListType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Project" maxOccurs="unbounded">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="Id" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                   &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                   &lt;element name="BaselineProject" maxOccurs="unbounded" minOccurs="0">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;element name="BaselineTypeName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                             &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                           &lt;/sequence>
 *                           &lt;attribute name="ObjectId" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *                 &lt;attribute name="ObjectId" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD) @XmlType(name = "ProjectListType", propOrder =
{
   "project"
}) public class ProjectListType
{

   @XmlElement(name = "Project", required = true) protected List<ProjectListType.Project> project;

   /**
    * Gets the value of the project property.
    *
    * <p>
    * This accessor method returns a reference to the live list,
    * not a snapshot. Therefore any modification you make to the
    * returned list will be present inside the JAXB object.
    * This is why there is not a <CODE>set</CODE> method for the project property.
    *
    * <p>
    * For example, to add a new item, do as follows:
    * <pre>
    *    getProject().add(newItem);
    * </pre>
    *
    *
    * <p>
    * Objects of the following type(s) are allowed in the list
    * {@link ProjectListType.Project }
    *
    *
    */
   public List<ProjectListType.Project> getProject()
   {
      if (project == null)
      {
         project = new ArrayList<ProjectListType.Project>();
      }
      return this.project;
   }

   /**
    * <p>Java class for anonymous complex type.
    *
    * <p>The following schema fragment specifies the expected content contained within this class.
    *
    * <pre>
    * &lt;complexType>
    *   &lt;complexContent>
    *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
    *       &lt;sequence>
    *         &lt;element name="Id" type="{http://www.w3.org/2001/XMLSchema}string"/>
    *         &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string"/>
    *         &lt;element name="BaselineProject" maxOccurs="unbounded" minOccurs="0">
    *           &lt;complexType>
    *             &lt;complexContent>
    *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
    *                 &lt;sequence>
    *                   &lt;element name="BaselineTypeName" type="{http://www.w3.org/2001/XMLSchema}string"/>
    *                   &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string"/>
    *                 &lt;/sequence>
    *                 &lt;attribute name="ObjectId" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
    *               &lt;/restriction>
    *             &lt;/complexContent>
    *           &lt;/complexType>
    *         &lt;/element>
    *       &lt;/sequence>
    *       &lt;attribute name="ObjectId" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
    *     &lt;/restriction>
    *   &lt;/complexContent>
    * &lt;/complexType>
    * </pre>
    *
    *
    */
   @XmlAccessorType(XmlAccessType.FIELD) @XmlType(name = "", propOrder =
   {
      "id",
      "name",
      "baselineProject"
   }) public static class Project
   {

      @XmlElement(name = "Id", required = true) protected String id;
      @XmlElement(name = "Name", required = true) protected String name;
      @XmlElement(name = "BaselineProject") protected List<ProjectListType.Project.BaselineProject> baselineProject;
      @XmlAttribute(name = "ObjectId", required = true) protected int objectId;

      /**
       * Gets the value of the id property.
       *
       * @return
       *     possible object is
       *     {@link String }
       *
       */
      public String getId()
      {
         return id;
      }

      /**
       * Sets the value of the id property.
       *
       * @param value
       *     allowed object is
       *     {@link String }
       *
       */
      public void setId(String value)
      {
         this.id = value;
      }

      /**
       * Gets the value of the name property.
       *
       * @return
       *     possible object is
       *     {@link String }
       *
       */
      public String getName()
      {
         return name;
      }

      /**
       * Sets the value of the name property.
       *
       * @param value
       *     allowed object is
       *     {@link String }
       *
       */
      public void setName(String value)
      {
         this.name = value;
      }

      /**
       * Gets the value of the baselineProject property.
       *
       * <p>
       * This accessor method returns a reference to the live list,
       * not a snapshot. Therefore any modification you make to the
       * returned list will be present inside the JAXB object.
       * This is why there is not a <CODE>set</CODE> method for the baselineProject property.
       *
       * <p>
       * For example, to add a new item, do as follows:
       * <pre>
       *    getBaselineProject().add(newItem);
       * </pre>
       *
       *
       * <p>
       * Objects of the following type(s) are allowed in the list
       * {@link ProjectListType.Project.BaselineProject }
       *
       *
       */
      public List<ProjectListType.Project.BaselineProject> getBaselineProject()
      {
         if (baselineProject == null)
         {
            baselineProject = new ArrayList<ProjectListType.Project.BaselineProject>();
         }
         return this.baselineProject;
      }

      /**
       * Gets the value of the objectId property.
       *
       */
      public int getObjectId()
      {
         return objectId;
      }

      /**
       * Sets the value of the objectId property.
       *
       */
      public void setObjectId(int value)
      {
         this.objectId = value;
      }

      /**
       * <p>Java class for anonymous complex type.
       *
       * <p>The following schema fragment specifies the expected content contained within this class.
       *
       * <pre>
       * &lt;complexType>
       *   &lt;complexContent>
       *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
       *       &lt;sequence>
       *         &lt;element name="BaselineTypeName" type="{http://www.w3.org/2001/XMLSchema}string"/>
       *         &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string"/>
       *       &lt;/sequence>
       *       &lt;attribute name="ObjectId" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
       *     &lt;/restriction>
       *   &lt;/complexContent>
       * &lt;/complexType>
       * </pre>
       *
       *
       */
      @XmlAccessorType(XmlAccessType.FIELD) @XmlType(name = "", propOrder =
      {
         "baselineTypeName",
         "name"
      }) public static class BaselineProject
      {

         @XmlElement(name = "BaselineTypeName", required = true) protected String baselineTypeName;
         @XmlElement(name = "Name", required = true) protected String name;
         @XmlAttribute(name = "ObjectId", required = true) protected int objectId;

         /**
          * Gets the value of the baselineTypeName property.
          *
          * @return
          *     possible object is
          *     {@link String }
          *
          */
         public String getBaselineTypeName()
         {
            return baselineTypeName;
         }

         /**
          * Sets the value of the baselineTypeName property.
          *
          * @param value
          *     allowed object is
          *     {@link String }
          *
          */
         public void setBaselineTypeName(String value)
         {
            this.baselineTypeName = value;
         }

         /**
          * Gets the value of the name property.
          *
          * @return
          *     possible object is
          *     {@link String }
          *
          */
         public String getName()
         {
            return name;
         }

         /**
          * Sets the value of the name property.
          *
          * @param value
          *     allowed object is
          *     {@link String }
          *
          */
         public void setName(String value)
         {
            this.name = value;
         }

         /**
          * Gets the value of the objectId property.
          *
          */
         public int getObjectId()
         {
            return objectId;
         }

         /**
          * Sets the value of the objectId property.
          *
          */
         public void setObjectId(int value)
         {
            this.objectId = value;
         }

      }

   }

}
