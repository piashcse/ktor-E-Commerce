package com.piashcse.utils.extension

import com.piashcse.utils.CommonException

fun String.isNotExistException(): CommonException {
    throw CommonException("$this is not Exist")
}

fun String.alreadyExistException(secondaryInfo: String = ""): CommonException {
    if (secondaryInfo.isNullOrEmpty())
        throw CommonException("$this is already Exist")
    else
        throw CommonException("$this $secondaryInfo is already Exist")
}