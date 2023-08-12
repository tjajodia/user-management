package com.learnings.self.ddb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import java.io.Serializable;

@DynamoDBTable(tableName = "UserTable")
public class UserDao implements Serializable {

    @DynamoDBAttribute
    private String userId;
    @DynamoDBAttribute
    private String firstName;
    @DynamoDBAttribute
    private String lastName;
    @DynamoDBAttribute
    private String emailAddress;
    @DynamoDBAttribute
    private String userName;
    @DynamoDBAttribute
    private String phoneNumber;
    @DynamoDBAttribute
    private String address;
}

