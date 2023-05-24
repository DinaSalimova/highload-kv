package com.company.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * represent query parameter.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Query {
    /**
     * if we have ack successful answers then we can say that query to update/delete/get is success.
     * We do not have to wait until all replicas will be updated. It's enough to have ack
     * successful responses
     */
    private int ack;

    /**
     * How many nodes we want to update (it can be less or equals that common amount of nodes in cluster)
     */
    private int from;

    /**
     * key of data (currently name of the file) that we want to update
     */
    private String id;

    /**
     * needs only for upsert, body which we sent to update or insert
     */
    private byte[] body;

    /**
     * if it's proxy than from current node queries will be sent to other nodes.
     * If it's not that query will be sent to the current node to update/delete/get data
     */
    private boolean isProxy;

    public Query(String id, String replicas, String isProxy) {
        if (id.isBlank()) {
            throw new IllegalArgumentException();
        }
        this.id = id;
        if (replicas == null) {
            this.isProxy = false;
            return;
        }
        if (isProxy == null) {
            this.isProxy = true;
        } else {
            this.isProxy = Boolean.parseBoolean(isProxy);
        }
        if (replicas.isBlank()) {
            throw new IllegalArgumentException();
        }
        int slashIndex = replicas.indexOf("/");
        this.ack = Integer.parseInt(replicas.substring(0, slashIndex));
        this.from = Integer.parseInt(replicas.substring(slashIndex + 1));
        if (ack == 0 || from < ack || from == 0) {
            throw new IllegalArgumentException();
        }
    }

}
