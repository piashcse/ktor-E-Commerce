package com.piashcse.utils.extension

import com.piashcse.utils.CommonException

fun String.isNotExistException(): CommonException {
    throw CommonException("$this is not Exist")
}

fun String.alreadyExistException(): CommonException {
    throw CommonException("$this is not Exist")
}