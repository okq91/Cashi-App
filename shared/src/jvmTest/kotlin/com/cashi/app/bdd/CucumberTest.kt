package com.cashi.app.bdd

import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/** Runs every `.feature` file under `src/jvmTest/resources/features` on the JUnit Platform. */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.cashi.app.bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
class CucumberTest
