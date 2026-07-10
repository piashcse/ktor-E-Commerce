package com.piashcse.feature.policy

class PolicyService(private val repo: PolicyRepository) : PolicyRepository by repo
