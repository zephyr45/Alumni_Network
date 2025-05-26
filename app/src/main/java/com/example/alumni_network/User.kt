package com.example.alumni_network

data class User(var FirstName:String ?=null,var LastName:String?=null,
    val PhoneNo:String?=null,val currentWorking:String?=null,val JobTitle:String?=null,val CompanyName:String?=null,val city:String?=null,
   val industry:String?=null)

