package com.appliedrec.veridregistry

object RandomNameGenerator {

    private val adjectives = listOf(
        "Adventurous", "Agile", "Ancient", "Angry", "Bashful", "Blue", "Bold", "Bouncy", "Brave", "Bright",
        "Busy", "Calm", "Cheery", "Clever", "Cloudy", "Cool", "Curious", "Daring", "Dazzling", "Delightful",
        "Determined", "Dizzy", "Dreamy", "Eager", "Fancy", "Fast", "Fierce", "Fluffy", "Friendly", "Funny",
        "Gentle", "Giant", "Giggly", "Glowing", "Graceful", "Grumpy", "Happy", "Helpful", "Honest", "Hungry",
        "Jolly", "Joyful", "Kind", "Lazy", "Light", "Loud", "Lucky", "Lush", "Magical", "Mellow", "Mighty",
        "Misty", "Noble", "Noisy", "Playful", "Proud", "Quick", "Quiet", "Quirky", "Radiant", "Sleepy", "Sneaky"
    )

    private val nouns = listOf(
        "Antelope", "Badger", "Bear", "Beaver", "Bison", "Butterfly", "Camel", "Cat", "Chameleon", "Cheetah",
        "Cobra", "Coyote", "Crane", "Crocodile", "Deer", "Dolphin", "Dragon", "Eagle", "Elephant", "Falcon",
        "Ferret", "Flamingo", "Fox", "Frog", "Giraffe", "Goat", "Goose", "Hedgehog", "Jaguar", "Kangaroo",
        "Koala", "Lemur", "Leopard", "Lion", "Llama", "Monkey", "Otter", "Owl", "Panda", "Panther"
    )

    fun generateRandomName(): String {
        val adjective = adjectives.random()
        val noun = nouns.random()
        return "$adjective $noun"
    }
}