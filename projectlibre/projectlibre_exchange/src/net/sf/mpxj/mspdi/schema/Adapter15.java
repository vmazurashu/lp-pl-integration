//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2011.11.14 at 02:49:33 PM GMT
//

package net.sf.mpxj.mspdi.schema;

import javax.xml.bind.annotation.adapters.XmlAdapter;

@SuppressWarnings("all") public class Adapter15 extends XmlAdapter<String, Integer>
{

   public Integer unmarshal(String value)
   {
      return (net.sf.mpxj.mspdi.DatatypeConverter.parseTaskUID(value));
   }

   public String marshal(Integer value)
   {
      return (net.sf.mpxj.mspdi.DatatypeConverter.printTaskUID(value));
   }

}
