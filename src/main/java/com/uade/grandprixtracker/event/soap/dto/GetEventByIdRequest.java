package com.uade.grandprixtracker.event.soap.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "getEventByIdRequest", namespace = "http://uade.com/grandprixtracker/soap/events")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetEventByIdRequest {

    @XmlElement(name = "id", required = true)
    private String id;

    public GetEventByIdRequest() {}

    public GetEventByIdRequest(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
