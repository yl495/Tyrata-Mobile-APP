package edu.duke.ece651.tyrata.communication;

/*Created by Ming on 2018-03-29.
* used to get trace from the database and then send to the server
*/

public class Trace_item {

    public String method;
    public String table_name;
    public int target_id;
    public String origin_info;
    public int id;

    // Constructor

    public Trace_item(int id, String method, String table_name, int target_id, String origin_info) {
        this.id = id;
        this.method = method;
        this.table_name = table_name;
        this.target_id = target_id;
        this.origin_info = origin_info;
    }

    public Trace_item(){this(0,"","",0,"");}

    public int getId() {
        return id;
    }

    public String getMethod() {
        return method;
    }

    public String getTable_name() {
        return table_name;
    }

    public int getTarget_id() {
        return target_id;
    }

    public String getOrigin_info() {
        return origin_info;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setTable_name(String table_name) {
        this.table_name = table_name;
    }

    public void setTarget_id(int target_id) {
        this.target_id = target_id;
    }

    public void setOrigin_info(String origin_info) {
        this.origin_info = origin_info;
    }

}
