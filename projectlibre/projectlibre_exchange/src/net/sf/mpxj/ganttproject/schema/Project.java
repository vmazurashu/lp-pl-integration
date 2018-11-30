//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2017.12.28 at 05:49:44 PM GMT
//

package net.sf.mpxj.ganttproject.schema;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="view" type="{}view" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="calendars" type="{}calendars"/>
 *         &lt;element name="tasks" type="{}tasks"/>
 *         &lt;element name="resources" type="{}resources"/>
 *         &lt;element name="allocations" type="{}allocations"/>
 *         &lt;element name="vacations" type="{}vacations"/>
 *         &lt;element name="previous" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="roles" type="{}roles" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="company" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="webLink" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="view-date" type="{http://www.w3.org/2001/XMLSchema}date" />
 *       &lt;attribute name="view-index" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="gantt-divider-location" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="resource-divider-location" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="version" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="locale" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD) @XmlType(name = "", propOrder =
{
   "description",
   "view",
   "calendars",
   "tasks",
   "resources",
   "allocations",
   "vacations",
   "previous",
   "roles"
}) @XmlRootElement(name = "project") public class Project
{

   @XmlElement(required = true) protected String description;
   protected List<View> view;
   @XmlElement(required = true) protected Calendars calendars;
   @XmlElement(required = true) protected Tasks tasks;
   @XmlElement(required = true) protected Resources resources;
   @XmlElement(required = true) protected Allocations allocations;
   @XmlElement(required = true) protected Vacations vacations;
   @XmlElement(required = true) protected String previous;
   protected List<Roles> roles;
   @XmlAttribute(name = "name") protected String name;
   @XmlAttribute(name = "company") protected String company;
   @XmlAttribute(name = "webLink") @XmlSchemaType(name = "anyURI") protected String webLink;
   @XmlAttribute(name = "view-date") @XmlJavaTypeAdapter(Adapter1.class) @XmlSchemaType(name = "date") protected Date viewDate;
   @XmlAttribute(name = "view-index") protected Integer viewIndex;
   @XmlAttribute(name = "gantt-divider-location") protected Integer ganttDividerLocation;
   @XmlAttribute(name = "resource-divider-location") protected Integer resourceDividerLocation;
   @XmlAttribute(name = "version") protected String version;
   @XmlAttribute(name = "locale") protected String locale;

   /**
    * Gets the value of the description property.
    * 
    * @return
    *     possible object is
    *     {@link String }
    *     
    */
   public String getDescription()
   {
      return description;
   }

   /**
    * Sets the value of the description property.
    * 
    * @param value
    *     allowed object is
    *     {@link String }
    *     
    */
   public void setDescription(String value)
   {
      this.description = value;
   }

   /**
    * Gets the value of the view property.
    * 
    * <p>
    * This accessor method returns a reference to the live list,
    * not a snapshot. Therefore any modification you make to the
    * returned list will be present inside the JAXB object.
    * This is why there is not a <CODE>set</CODE> method for the view property.
    * 
    * <p>
    * For example, to add a new item, do as follows:
    * <pre>
    *    getView().add(newItem);
    * </pre>
    * 
    * 
    * <p>
    * Objects of the following type(s) are allowed in the list
    * {@link View }
    * 
    * 
    */
   public List<View> getView()
   {
      if (view == null)
      {
         view = new ArrayList<View>();
      }
      return this.view;
   }

   /**
    * Gets the value of the calendars property.
    * 
    * @return
    *     possible object is
    *     {@link Calendars }
    *     
    */
   public Calendars getCalendars()
   {
      return calendars;
   }

   /**
    * Sets the value of the calendars property.
    * 
    * @param value
    *     allowed object is
    *     {@link Calendars }
    *     
    */
   public void setCalendars(Calendars value)
   {
      this.calendars = value;
   }

   /**
    * Gets the value of the tasks property.
    * 
    * @return
    *     possible object is
    *     {@link Tasks }
    *     
    */
   public Tasks getTasks()
   {
      return tasks;
   }

   /**
    * Sets the value of the tasks property.
    * 
    * @param value
    *     allowed object is
    *     {@link Tasks }
    *     
    */
   public void setTasks(Tasks value)
   {
      this.tasks = value;
   }

   /**
    * Gets the value of the resources property.
    * 
    * @return
    *     possible object is
    *     {@link Resources }
    *     
    */
   public Resources getResources()
   {
      return resources;
   }

   /**
    * Sets the value of the resources property.
    * 
    * @param value
    *     allowed object is
    *     {@link Resources }
    *     
    */
   public void setResources(Resources value)
   {
      this.resources = value;
   }

   /**
    * Gets the value of the allocations property.
    * 
    * @return
    *     possible object is
    *     {@link Allocations }
    *     
    */
   public Allocations getAllocations()
   {
      return allocations;
   }

   /**
    * Sets the value of the allocations property.
    * 
    * @param value
    *     allowed object is
    *     {@link Allocations }
    *     
    */
   public void setAllocations(Allocations value)
   {
      this.allocations = value;
   }

   /**
    * Gets the value of the vacations property.
    * 
    * @return
    *     possible object is
    *     {@link Vacations }
    *     
    */
   public Vacations getVacations()
   {
      return vacations;
   }

   /**
    * Sets the value of the vacations property.
    * 
    * @param value
    *     allowed object is
    *     {@link Vacations }
    *     
    */
   public void setVacations(Vacations value)
   {
      this.vacations = value;
   }

   /**
    * Gets the value of the previous property.
    * 
    * @return
    *     possible object is
    *     {@link String }
    *     
    */
   public String getPrevious()
   {
      return previous;
   }

   /**
    * Sets the value of the previous property.
    * 
    * @param value
    *     allowed object is
    *     {@link String }
    *     
    */
   public void setPrevious(String value)
   {
      this.previous = value;
   }

   /**
    * Gets the value of the roles property.
    * 
    * <p>
    * This accessor method returns a reference to the live list,
    * not a snapshot. Therefore any modification you make to the
    * returned list will be present inside the JAXB object.
    * This is why there is not a <CODE>set</CODE> method for the roles property.
    * 
    * <p>
    * For example, to add a new item, do as follows:
    * <pre>
    *    getRoles().add(newItem);
    * </pre>
    * 
    * 
    * <p>
    * Objects of the following type(s) are allowed in the list
    * {@link Roles }
    * 
    * 
    */
   public List<Roles> getRoles()
   {
      if (roles == null)
      {
         roles = new ArrayList<Roles>();
      }
      return this.roles;
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
    * Gets the value of the company property.
    * 
    * @return
    *     possible object is
    *     {@link String }
    *     
    */
   public String getCompany()
   {
      return company;
   }

   /**
    * Sets the value of the company property.
    * 
    * @param value
    *     allowed object is
    *     {@link String }
    *     
    */
   public void setCompany(String value)
   {
      this.company = value;
   }

   /**
    * Gets the value of the webLink property.
    * 
    * @return
    *     possible object is
    *     {@link String }
    *     
    */
   public String getWebLink()
   {
      return webLink;
   }

   /**
    * Sets the value of the webLink property.
    * 
    * @param value
    *     allowed object is
    *     {@link String }
    *     
    */
   public void setWebLink(String value)
   {
      this.webLink = value;
   }

   /**
    * Gets the value of the viewDate property.
    * 
    * @return
    *     possible object is
    *     {@link String }
    *     
    */
   public Date getViewDate()
   {
      return viewDate;
   }

   /**
    * Sets the value of the viewDate property.
    * 
    * @param value
    *     allowed object is
    *     {@link String }
    *     
    */
   public void setViewDate(Date value)
   {
      this.viewDate = value;
   }

   /**
    * Gets the value of the viewIndex property.
    * 
    * @return
    *     possible object is
    *     {@link Integer }
    *     
    */
   public Integer getViewIndex()
   {
      return viewIndex;
   }

   /**
    * Sets the value of the viewIndex property.
    * 
    * @param value
    *     allowed object is
    *     {@link Integer }
    *     
    */
   public void setViewIndex(Integer value)
   {
      this.viewIndex = value;
   }

   /**
    * Gets the value of the ganttDividerLocation property.
    * 
    * @return
    *     possible object is
    *     {@link Integer }
    *     
    */
   public Integer getGanttDividerLocation()
   {
      return ganttDividerLocation;
   }

   /**
    * Sets the value of the ganttDividerLocation property.
    * 
    * @param value
    *     allowed object is
    *     {@link Integer }
    *     
    */
   public void setGanttDividerLocation(Integer value)
   {
      this.ganttDividerLocation = value;
   }

   /**
    * Gets the value of the resourceDividerLocation property.
    * 
    * @return
    *     possible object is
    *     {@link Integer }
    *     
    */
   public Integer getResourceDividerLocation()
   {
      return resourceDividerLocation;
   }

   /**
    * Sets the value of the resourceDividerLocation property.
    * 
    * @param value
    *     allowed object is
    *     {@link Integer }
    *     
    */
   public void setResourceDividerLocation(Integer value)
   {
      this.resourceDividerLocation = value;
   }

   /**
    * Gets the value of the version property.
    * 
    * @return
    *     possible object is
    *     {@link String }
    *     
    */
   public String getVersion()
   {
      return version;
   }

   /**
    * Sets the value of the version property.
    * 
    * @param value
    *     allowed object is
    *     {@link String }
    *     
    */
   public void setVersion(String value)
   {
      this.version = value;
   }

   /**
    * Gets the value of the locale property.
    * 
    * @return
    *     possible object is
    *     {@link String }
    *     
    */
   public String getLocale()
   {
      return locale;
   }

   /**
    * Sets the value of the locale property.
    * 
    * @param value
    *     allowed object is
    *     {@link String }
    *     
    */
   public void setLocale(String value)
   {
      this.locale = value;
   }

}
