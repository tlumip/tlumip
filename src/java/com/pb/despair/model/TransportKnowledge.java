package com.pb.despair.model;

import com.pb.despair.pi.TAZ;



/**
 * There will always be one TransportKnowlege which represents the base knowledge of everyone.   This is stored in the class
 * variable globalTransportKnowledge. The base knowledge of "how easy it is to get from here to there" is considered global
 * knowledge.  We are not modelling how actors aquire the knowledge of the characteristics of the transport network.  Instead,
 * each actor can consult this global object to find out about the attributes of travel for different trips at different times.
 * The design, however, is such that eventually actors or agents or factors or households could have a pointer to their own
 * TransportKnowledge, and they could share that knowledge as they go about their business.
 * <p>For ODOT, TransportKnowledge will never be more than a year old since travel conditions are re-simulated every year.
 * @author J. Abraham
 * @clientCardinality 1..*
 * @supplierCardinality 1
 */
public abstract class TransportKnowledge {

    /**
     * This routine sends the following request to the Transport Knowledge:
     * "if I have certain travel preferences regarding switching between time-of-day,
     * time-of-week, mode etc., then give me a number that represents how hard it is for
     * me to travel from point x to point y."
     */
    public abstract double getUtility(AbstractTAZ from, AbstractTAZ to, TravelUtilityCalculatorInterface tp, boolean useRouteChoice);


    public static TransportKnowledge globalTransportKnowledge = null;

    public String toString() {
        if (this == globalTransportKnowledge) {
            return "global transport knowledge object";
        } else {
            return "special transport knowledge object (non global)";
        }
    }

    /**
     * @param taz
     * @param abstractTAZ
     * @param interface1
     * @param b
     * @return
     */
    public abstract double[] getUtilityComponents(AbstractTAZ from, AbstractTAZ to, TravelUtilityCalculatorInterface interface1, boolean useRouteChoice);
}

