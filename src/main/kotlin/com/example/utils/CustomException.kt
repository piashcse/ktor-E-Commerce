package com.example.utils

class UserNotExistException : Exception()
class UserTypeException : Exception()
class EmailNotExist : Exception()
class PasswordNotMatch : Exception()
class CommonException(itemName: String) : Exception(itemName)
class NotExist(itemName: String) : Exception(itemName)
