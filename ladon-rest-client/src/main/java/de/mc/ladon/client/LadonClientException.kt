package de.mc.ladon.client

import de.mc.ladon.s3server.jaxb.entities.Error
import java.lang.RuntimeException

class LadonClientException(val error: Error) :
        RuntimeException("${error.message}") {
}
