package com.thatdot.quine.graph

import com.google.common.hash.Hashing.murmur3_128
import org.scalacheck.rng.Seed
import org.scalatest.flatspec.AnyFlatSpec

import com.thatdot.quine.model.{HalfEdge, PropertyValue, QuineId}

class GraphNodeHashCodeTest extends AnyFlatSpec with HalfEdgeGen with ArbitraryInstances {
  it must "generate stable identifiers for arbitrary values" in {
    val hasher = murmur3_128.newHasher
    val times = 1000
    for (i <- 0 until times) {
      val seed = Seed(i.toLong)
      val qid = Generators.generate1[QuineId](size = 100, seed = seed)
      val propertiesCount = 10
      val propertyKeys = Generators.generateN[String](n = propertiesCount, size = 10, seed = seed)
      val propertyValues = Generators.generateN[PropertyValue](n = propertiesCount, size = 50, seed = seed)
      val properties = propertyKeys.map(Symbol.apply).zip(propertyValues).toMap
      val edges = Generators.generateN[HalfEdge](n = 10, size = 100, seed = seed)
      val graphNodeHashCode = GraphNodeHashCode(qid, properties, edges)
      hasher.putLong(graphNodeHashCode.value)
    }
    assert(-7866497942312184115L === hasher.hash().asLong())
  }
}
