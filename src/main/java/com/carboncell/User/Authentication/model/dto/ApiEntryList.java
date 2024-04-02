package com.carboncell.User.Authentication.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiEntryList implements Serializable {
    private static final long serialVersionUID = 328943893449L;
    private int count;
    private List<ApiEntry> entries;
}