package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.counting.io

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

import org.jboss.marshalling.Marshalling
import org.jboss.marshalling.MarshallingConfiguration

import org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.counting.Counter

object CounterIO {

    private val marshallerFactory = Marshalling.getProvidedMarshallerFactory("river")
    private val configuration = MarshallingConfiguration()

    init {
        configuration.version = 3
    }

    fun readCounter(file: File): Counter? {
        println("Reading counter from: $file")
        return try {
            FileInputStream(file).use { inputStream ->
                val unmarshaller = marshallerFactory.createUnmarshaller(configuration)
                unmarshaller.start(Marshalling.createByteInput(inputStream))
                val counter = unmarshaller.readObject() as Counter
                unmarshaller.finish()
                inputStream.close()
                counter
            }
        } catch (e: IOException) {
            System.err.print("Un-marshalling failed: ")
            e.printStackTrace()
            null
        }
    }

    fun writeCounter(counter: Counter, file: File) {
        println("Writing counter to: $file")
        try {
            FileOutputStream(file).use { outputStream ->
                val marshaller = marshallerFactory.createMarshaller(configuration)
                marshaller.start(Marshalling.createByteOutput(outputStream))
                marshaller.writeObject(counter)
                marshaller.finish()
                outputStream.close()
            }
        } catch (e: IOException) {
            System.err.print("Marshalling failed: ")
            e.printStackTrace()
        }

    }
}