# SC Vote - Design

This primarily uses a document table to store documents of various types and subtypes.  Partitioning results in each doctype/subtype being treated like a separate table for performance.  

Within it, there are three OBJECT fields, each being its own document, per se.  `core` is designed to hold pre-defined fields, although it is technically extensible at runtime.  These fields should be re-usable across various doctypes, though, as they will share the same field data types.  `ext` is designed to provide a way for externally defined documents to be inserted.  They do not result in it defining data types for fields, so future use of the same field name within it does not need to conform to a previously used data type.  `meta` is for internal use only, such as internal auditing fields. 

`owner_mid` is designed to support data ownership, and can be used to enforce data security.  `mid` stands for Member ID, which, in a real-world app, can be an externally provided identifier provided by an identity service, and can potentially identify a person, organization or service.   

## Table definition

    DROP TABLE appdoc;
    
    CREATE TABLE appdoc IF NOT EXISTS (
			owner_mid string NOT NULL,
			core object(dynamic) as (
				 doctype string NOT NULL,
				 docsubtype string,
			   sid string,
			   qty long,
			   cmid string,
			   ceid string,
				 devid string,
				 token string,
				 content string,
			   email string,
			   name string,
			   firstname string,
			   lastname string,
			   middlename string 
			) NOT NULL,
			ext object(ignored),
			meta object(dynamic) as (
				client_ip ip,
				created timestamp
			) NOT NULL,
			created_year AS EXTRACT( year from meta['created'] ),
			created_month AS EXTRACT( month from meta['created'] ),
			created_day AS EXTRACT( day from meta['created'] ),
			created_hour AS EXTRACT( hour from meta['created'] ),
		  doctype AS core['doctype'],
		  docsubtype AS core['docsubtype']
		) PARTITIONED BY (doctype, docsubtype, created_year, created_month) ;

## Runtime Data Model Generation

**Don't forget to update AppDB#upgradeSchema when you change table schema.**

Because the automated data model generation is still a WIP, it is currently available by manually invoking the `scs/vote/v1/test/db` REST service after deploy.  In the future, it will become an automatic post-deploy startup event requiring no human interaction.  

## CrateDB Limitations

* CrateDB does not support default values, or else DEFAULT CURRENT_TIMESTAMP would work. 
* There is a bug that prevents the enforcement of NOT NULL on doctype.  It creates the table successfully, but is_nullable is 't' in the definition.  

## SQL

Sample inserts:

    INSERT INTO appdoc(owner_mid, core) 
      VALUES('me', {});

	select column_name, data_type, column_default, is_nullable from information_schema.columns where table_name like 'app%';     

## PARTITIONED BY and PRIMARY KEY

This table does not currently include a PRIMARY KEY because, according to CrateDB documentation:

	if the table has a PRIMARY KEY Constraint the columns in PARTITIONED clause have to be part of it
	
The down-side to not having one is that if you INSERT/UPDATE, a following SELECT is not guaranteed to immediately include it.  It can take up to 1 second for it to be available for queries.   

You can work around that by having a generated PRIMARY KEY such as a UIID.  Further experimentation is required on the impact and potential benefits of adding a primary key.  For now, we'll live without one and be careful with assumptions about querying after INSERT/UPDATE, which you will have to do for any multi-row query even with a primary key.  

The primary benefit of partitioning is that each partition acts like a separate table for performance.  That is what makes a table partitioned up by `doctype` and `docsubtype` highly performant for adding new doctypes.  In effect, new doc types do not impact the performance of other doc types.  Including year and month in the partitioning permits current month queries to be faster than older data queries, as well as efficient month-by-month viewing of data. 

## Tuning

TODO: Changing the number of replicas/shards?   
    
## References

[Create Data Types](https://crate.io/docs/crate/reference/sql/data_types.html#sql-ddl-datatypes)

https://github.com/wildfly-swarm/wildfly-swarm-examples/blob/master/jaxrs/health/src/main/java/org/wildfly/swarm/examples/jaxrs/health/CORSFilter.java
