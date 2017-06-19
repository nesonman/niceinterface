package com.example.example.myapplication.utils;

import com.google.common.collect.Multimap;

import java.util.Map;

public class IndexPair {
    public Multimap<String, String> lp1;
    public Multimap<String, String> lp2;
    public Map<String, String> fileToRename;

    public IndexPair(Multimap<String, String> lookup1, Multimap<String, String> lookup2, Map<String, String> fileToRename) {
        this.lp1 = lookup1;
        this.lp2 = lookup2;
        this.fileToRename = fileToRename;
    }
}