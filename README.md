Description to the project:
It is a key-value storage system that distributes data across multiple storage nodes. 

* HTTP `GET /v0/entity?id=<ID>` -- get data by key `<ID>`. returns `200 OK` and data or `404 Not Found`.
* HTTP `PUT /v0/entity?id=<ID>` -- create/update (upsert) data by key `<ID>`. Returns `201 Created`.
* HTTP `DELETE /v0/entity?id=<ID>` -- delete data by key `<ID>`. Returns `202 Accepted`.

Implemented support of cluster that contains few nodes. Nodes interact between each other via HTTP API.
To implement this factory `KVServiceFactory` gets topologies. Topologies is a list of coordinates of all nodes.
It has a format `http://<host>:<port>`.
HTTP API has also parameter of query `replicas`. It contains amount of nodes, which must confirm the operation for it to be considered successful.
Value `replicas` writes as  `ack/from`, where:
* `ack` -- amount of successful answers
* `from` -- from how many nodes

API:
* HTTP `GET /v0/entity?id=<ID>[&replicas=ack/from]` -- getting answer by key `<ID>`. Returns:
  * `200 OK` and data, if they answered at least `ack` from `from` replicas
  * `404 Not Found`, no one from `ack` replicas, that returns answer, do not have data 
  (or data **was deleted** on one of the `ack` replica)
  * `504 Not Enough Replicas`, if it does not get `200`/`404` from `ack` replicas from the entire set of `from` replicas

* HTTP `PUT /v0/entity?id=<ID>[&replicas=ack/from]` -- create/update (upsert) data by key `<ID>`. Returns:
  * `201 Created`, if at least `ack` from `from` replicas confirmed the operation
  * `504 Not Enough Replicas`, if there are no `ack` confirmations from the entire set of `from` replicas

* HTTP `DELETE /v0/entity?id=<ID>[&replicas=ack/from]` -- delete data by key `<ID>`. Returns:
  * `202 Accepted`, if at least `ack` from `from` replicas confirmed the operation
  * `504 Not Enough Replicas`, if there are no `ack` confirmations from the entire set of `from` replicas
