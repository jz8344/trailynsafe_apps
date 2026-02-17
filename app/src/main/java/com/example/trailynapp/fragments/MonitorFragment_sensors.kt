override fun onSensorChanged(event: SensorEvent?) {
    if (event == null) return

    when (event.sensor.type) {
        Sensor.TYPE_ACCELEROMETER -> {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        }
        Sensor.TYPE_MAGNETIC_FIELD -> {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }
    }

    updateOrientationAndRotateArrow()
}

override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

private fun updateOrientationAndRotateArrow() {
    SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)

    SensorManager.getOrientation(rotationMatrix, orientationAngles)

    currentAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

    if (currentAzimuth < 0) {
        currentAzimuth += 360f
    }

    selectedLocation?.let { destination ->
        googleMap?.myLocation?.let { myLoc ->
            val currentLocation = LatLng(myLoc.latitude, myLoc.longitude)
            val bearing = calculateBearing(currentLocation, destination)

            var rotation = bearing - currentAzimuth

            if (rotation < 0) {
                rotation += 360f
            }
            if (rotation > 360) {
                rotation -= 360f
            }

            rotateArrow(rotation)
        }
    }
}

private fun calculateBearing(from: LatLng, to: LatLng): Float {
    val lat1 = Math.toRadians(from.latitude)
    val lon1 = Math.toRadians(from.longitude)
    val lat2 = Math.toRadians(to.latitude)
    val lon2 = Math.toRadians(to.longitude)

    val dLon = lon2 - lon1

    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

    var bearing = Math.toDegrees(atan2(y, x))
    bearing = (bearing + 360) % 360

    return bearing.toFloat()
}

private fun rotateArrow(targetRotation: Float) {
    val rotateAnimation =
            RotateAnimation(
                    currentRotation,
                    targetRotation,
                    Animation.RELATIVE_TO_SELF,
                    0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f
            )

    rotateAnimation.duration = 200
    rotateAnimation.fillAfter = true

    navigationArrow.startAnimation(rotateAnimation)
    currentRotation = targetRotation
}

override fun onResume() {
    super.onResume()
    accelerometer?.also { acc ->
        sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_UI)
    }
    magnetometer?.also { mag ->
        sensorManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_UI)
    }
}

override fun onPause() {
    super.onPause()
    sensorManager.unregisterListener(this)
}
