
package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Unit;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
* Represents the need for goods within a <code>Colony</code>.
* <br><br>
* TODO: Deal in amounts of goods.
*/
public class GoodsWish extends Wish {
    private static final Logger logger = Logger.getLogger(GoodsWish.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private int goodsType;

    /**
    * Creates a new <code>GoodsWish</code>.
    *
    * @param aiMain The main AI-object.
    * @param destination The <code>Location</code> in which the
    *       {@link Wish#getTransportable transportable} assigned to
    *       this <code>GoodsWish</code> will have to reach.
    * @param value The value identifying the importance of
    *       this <code>Wish</code>.
    * @param goodsType The type of goods needed for releasing this wish
    *       completly.
    */
    public GoodsWish(AIMain aiMain, Location destination, int value, int goodsType) {
        super(aiMain);

        id = aiMain.getNextID();
        aiMain.addAIObject(id, this);

        this.destination = destination;
        this.value = value;
        this.goodsType = goodsType;
    }


    /**
    * Creates a new <code>GoodsWish</code> from the given XML-representation.
    *
    * @param aiMain The main AI-object.
    * @param element The root element for the XML-representation of a <code>GoodsWish</code>.
    */
    public GoodsWish(AIMain aiMain, Element element) {
        super(aiMain);
        aiMain.addAIObject(element.getAttribute("ID"), this);
        readFromXMLElement(element);
    }


    /**
    * Returns the type of unit needed for releasing this wish.
    * @return The {@link Unit#getType type of unit}.
    */
    public int getGoodsType() {
        return goodsType;
    }


    /**
     * Creates an XML-representation of this object.
     * @param document The <code>Document</code> in which
     *      the XML-representation should be created.
     * @return The XML-representation.
     */    
    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("ID", id);
        element.setAttribute("destination", destination.getID());
        if (transportable != null) {
            element.setAttribute("transportable", transportable.getID());
        }
        element.setAttribute("value", Integer.toString(value));

        element.setAttribute("goodsType", Integer.toString(goodsType));

        return element;
    }


    /**
     * Updates this object from an XML-representation of
     * a <code>GoodsWish</code>.
     * 
     * @param element The XML-representation.
     */    
    public void readFromXMLElement(Element element) {
        id = element.getAttribute("ID");
        destination = (Location) getAIMain().getFreeColGameObject(element.getAttribute("destination"));
        if (element.hasAttribute("transportable")) {
            transportable = (Transportable) getAIMain().getAIObject(element.getAttribute("transportable"));
        } else {
            transportable = null;
        }
        value = Integer.parseInt(element.getAttribute("value"));

        goodsType = Integer.parseInt(element.getAttribute("goodsType"));
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return "GoodsWish"
    */
    public static String getXMLElementTagName() {
        return "GoodsWish";
    }
}
