//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2017.12.28 at 05:49:44 PM GMT
//

package net.sf.mpxj.ganttproject.schema;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * <p>Java class for date complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="date">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="year" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="month" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="date" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@SuppressWarnings("all") @XmlAccessorType(XmlAccessType.FIELD) @XmlType(name = "date", propOrder =
{
   "value"
}) public class Date
{

   @XmlValue protected String value;
   @XmlAttribute(name = "year") protected String year;
   @XmlAttribute(name = "month") protected Integer month;
   @XmlAttribute(name = "date") protected Integer date;
   @XmlAttribute(name = "type") protected String type;

   /**
    * Gets the value of the value property.
    *
    * @return
    *     possible object is
    *     {@link String }
    *
    */
   public String getValue()
   {
      return value;
   }

   /**
    * Sets the value of the value property.
    *
    * @param value
    *     allowed object is
    *     {@link String }
    *
    */
   public void setValue(String value)
   {
      this.value = value;
   }

   /**
    * Gets the value of the year property.
    *
    * @return
    *     possible object is
    *     {@link String }
    *
    */
   public String getYear()
   {
      return year;
   }

   /**
    * Sets the value of the year property.
    *
    * @param value
    *     allowed object is
    *     {@link String }
    *
    */
   public void setYear(String value)
   {
      this.year = value;
   }

   /**
    * Gets the value of the month property.
    *
    * @return
    *     possible object is
    *     {@link Integer }
    *
    */
   public Integer getMonth()
   {
      return month;
   }

   /**
    * Sets the value of the month property.
    *
    * @param value
    *     allowed object is
    *     {@link Integer }
    *
    */
   public void setMonth(Integer value)
   {
      this.month = value;
   }

   /**
    * Gets the value of the date property.
    *
    * @return
    *     possible object is
    *     {@link Integer }
    *
    */
   public Integer getDate()
   {
      return date;
   }

   /**
    * Sets the value of the date property.
    *
    * @param value
    *     allowed object is
    *     {@link Integer }
    *
    */
   public void setDate(Integer value)
   {
      this.date = value;
   }

   /**
    * Gets the value of the type property.
    *
    * @return
    *     possible object is
    *     {@link String }
    *
    */
   public String getType()
   {
      return type;
   }

   /**
    * Sets the value of the type property.
    *
    * @param value
    *     allowed object is
    *     {@link String }
    *
    */
   public void setType(String value)
   {
      this.type = value;
   }

}