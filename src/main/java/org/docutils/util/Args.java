package org.docutils.util;

import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.List;

public class Args {
    @Parameter
    private List<String> parameters = new ArrayList<>();

    @Parameter(names = "--temporal")
    public boolean temporal = false;

    @Parameter(names = "--equivalence")
    public boolean equivalence = false;

    @Parameter(names = "--dep", variableArity = true)
    public List<String> dependencies = new ArrayList<>();

    @Parameter(names = "--keywords", variableArity = true)
    public List<String> keywords = new ArrayList<>();

    @Parameter(names = "--regex")
    public String regex = "";
}
