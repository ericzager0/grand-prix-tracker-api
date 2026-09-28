package com.uade.grandprixtracker.event.soap.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "getAllEventsRequest", namespace = "http://uade.com/grandprixtracker/soap/events")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetAllEventsRequest {

    public GetAllEventsRequest() {}
}
