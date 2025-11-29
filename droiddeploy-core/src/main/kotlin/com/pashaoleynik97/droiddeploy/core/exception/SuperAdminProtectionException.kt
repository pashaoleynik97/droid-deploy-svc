package com.pashaoleynik97.droiddeploy.core.exception

/**
 * Exception thrown when attempting to modify super admin's protected attributes
 */
class SuperAdminProtectionException : DroidDeployException("Super admin account cannot be modified")
