package com.isti.gmpegmm;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.EnumMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gov.usgs.earthquake.nshmp.gmm.Gmm;
import gov.usgs.earthquake.nshmp.model.GmmTree;
import gov.usgs.earthquake.nshmp.tree.LogicTree;

public class GmmUtil {
    private static final Map<Region, LogicTree<Gmm>> GMM_TREE_MAP;
    private static final String GMM_TREES = "/res/gmm-trees.json";
    static {
        InputStream is = GmmUtil.class.getResourceAsStream(GMM_TREES);
        GMM_TREE_MAP = new EnumMap<>(Region.class);
        try (Reader reader = new InputStreamReader(is)) {
            JsonArray jArray = JsonParser.parseReader(reader).getAsJsonArray();
            jArray.forEach(e -> {
                JsonObject o = e.getAsJsonObject();
                GMM_TREE_MAP.put(Region.valueOf(o.get("id").getAsString()), GmmTree.parseTree(o.get("tree")));
            });
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static LogicTree<Gmm> getGmmTree(Region region) {
        return GMM_TREE_MAP.get(region);
    }

    public static void main(String[] args) {
        GMM_TREE_MAP.entrySet().forEach(e -> {
            System.out.println(e.getKey());
            System.out.println(e.getValue());
        });
    }
}
