package com.example.utils

class UserNotExistException : Exception()
class UserTypeException : Exception()
class EmailNotExist : Exception()
class PasswordNotMatch : Exception()
class AlreadyExist(itemName: String) : Exception(itemName)
