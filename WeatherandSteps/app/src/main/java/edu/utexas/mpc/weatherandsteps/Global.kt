package edu.utexas.mpc.weatherandsteps

object Global {
    private var age: String? = null
    private var gender: String? = null
    private var height: String? = null
    private var weight: String? = null

    fun setAge(age: String) {
        this.age = age
    }

    fun getAge(): String? {
        return this.age
    }

    fun setGender(gender: String) {
        this.gender = gender
    }
    fun getGender():String? {
        return this.gender
    }

    fun getHeight(): String? {
        return this.height
    }

    fun setHeight(height: String) {
        this.height = height
    }

    fun getWeight(): String? {
        return this.weight
    }

    fun setWeight(weight: String) {
        this.weight = weight
    }
}