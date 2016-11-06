package io.iohk.iodb.bench

import java.io.File
import java.util.Random

import io.iohk.iodb._

case class BenchResult(storage: String, insertTime: Long, getTime: Long, storeSizeMb: Long)

/**
  * Benchmark for IODB
  */
object SimpleKVBench extends Benchmark{

  var updates = 0L
  var keyCount = 0L

  def main(args: Array[String]) {
    updates = args(0).toLong
    keyCount = args(1).toLong

    var dir = TestUtils.tempDir()
    val lb = bench(new LSMStore(dir, keySize = KeySize, keepSingleVersion = true,
      minMergeCount = 1
      //        minMergeSize = 128*1024,
      //        splitSize = 16*1024
    ), dir)
    printlnResult(lb)

    dir = TestUtils.tempDir()
    val rb = bench(new RocksStore(dir), dir)
    printlnResult(rb)

    if (lb.getTime < rb.getTime && lb.insertTime < rb.insertTime) {
      println("IODB won!")
    }
  }

  def bench(store: Store, dir: File): BenchResult = {
    val r = new Random(1)
    var version = 0
    //insert random values
    val insertTime = TestUtils.runningTimeUnit {
      for (i <- 0 until updates) {
        val toInsert = (0 until keyCount).map { a =>
          val k = randomKey(r)
          (k, k)
        }
        version += 1
        store.update(version, List.empty, toInsert)
      }
    }

    Thread.sleep(100000)

    val getTime = TestUtils.runningTimeUnit {
        val r = new Random(1)
        for (i <- 0 until updates) {
          val toGet = (0 until keyCount).map { i =>
            randomKey(r)
          }

          version += 1

          toGet.foreach { k =>
            assert(null != store.get(k))
          }
        }
    }

    val br = BenchResult(store.getClass.toString, insertTime, getTime, TestUtils.dirSize(dir) / (1024 * 1024))

    store.close()
    TestUtils.deleteRecur(dir)
    br
  }

  def randomKey(r: Random): ByteArrayWrapper = {
    val key = new Array[Byte](KeySize)
    r.nextBytes(key)
    ByteArrayWrapper(key)
  }

  def printlnResult(res: BenchResult): Unit = {
    println(s"Store: ${res.storage}")
    println(s"Insert time:  ${res.insertTime}")
    println(s"Get time: ${res.getTime}")
    println(s"Store size: ${res.storeSizeMb} MB")
  }
}
