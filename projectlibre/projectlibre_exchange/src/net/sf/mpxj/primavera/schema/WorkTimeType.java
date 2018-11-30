//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2017.09.18 at 02:35:45 PM BST
//

package net.sf.mpxj.primavera.schema;

import java.util.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * <p>Java class for WorkTimeType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="WorkTimeType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Start" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}time">
 *               &lt;pattern value="\d{2}:[03]0:00"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="Finish" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}time">
 *               &lt;pattern value="\d{2}:[25]9:00"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD) @XmlType(name = "WorkTimeType", propOrder =
{
   "start",
   "finish"
}) public class WorkTimeType
{

   @XmlElement(name = "Start", type = String.class) @XmlJavaTypeAdapter(Adapter2.class) protected Date start;
   @XmlElement(name = "Finish", type = String.class) @XmlJavaTypeAdapter(Adapter2.class) protected Date finish;

   /**
    * Gets the value of the start property.
    *
    * @return
    *     possible object is
    *     {@link String }
    *
    */
   public Date getStart()
   {
      return start;
   }

   /**
    * Sets the value of the start property.
    *
    * @param value
    *     allowed object is
    *     {@link String }
    *
    */
   public void setStart(Date value)
   {
      this.start = value;
   }

   /**
    * Gets the value of the finish property.
    *
    * @return
    *     possible object is
    *     {@link String }
    *
    */
   public Date getFinish()
   {
      return finish;
   }

   /**
    * Sets the value of the finish property.
    *
    * @param value
    *     allowed object is
    *     {@link String }
    *
    */
   public void setFinish(Date value)
   {
      this.finish = value;
   }

}
