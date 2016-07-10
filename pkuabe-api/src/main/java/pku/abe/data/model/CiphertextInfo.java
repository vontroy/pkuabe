package pku.abe.data.model;

import com.alibaba.fastjson.JSONObject;
import it.unisa.dia.gas.jpbc.Element;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vontroy on 7/10/16.
 */
public class CiphertextInfo {
    private PolicyInfo policy;
    private Map<String, Element> components;
    private byte[] load;
    private int[][] matrix;
    private Map<Integer, String> rho;

    public CiphertextInfo() {
        this.components = new HashMap<>();
    }

    public PolicyInfo getPolicy() {
        return policy;
    }

    public void setPolicy(PolicyInfo policy) {
        this.policy = policy;
    }

    public Map<String, Element> getComponents() {
        return components;
    }

    public void setComponents(Map<String, Element> components) {
        this.components = components;
    }

    public byte[] getLoad() {
        return load;
    }

    public void setLoad(byte[] load) {
        this.load = load;
    }

    public int[][] getMatrix() {
        return matrix;
    }

    public void setMatrix(int[][] matrix) {
        this.matrix = matrix;
    }

    public Map<Integer, String> getRho() {
        return rho;
    }

    public void setRho(Map<Integer, String> rho) {
        this.rho = rho;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("policy:" + policy + "\n");
        sb.append("Components:{\n");
        for(Map.Entry<String, Element> element : getComponents().entrySet()){
            sb.append(element.getKey() + "--->" + element.getValue() + "\n");
        }
        sb.append("}");
        return sb.toString();
    }

    public String toJSONString() {
        JSONObject obj = new JSONObject();
        obj.put("policy", policy.toString());

        for(Map.Entry<String, Element> entry : this.components.entrySet()){
            obj.put(entry.getKey(), entry.getValue().toBytes());
        }
        obj.put("load", load);

        return obj.toJSONString();
    }
}
