package com.pashaoleynik97.droiddeploy.core.exception

/**
 * Exception thrown when user attempts to modify their own active status
 */
class SelfModificationNotAllowedException : DroidDeployException("You cannot change your own active status")
