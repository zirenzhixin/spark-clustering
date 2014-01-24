package org.altic.spark.clustering

import org.apache.spark.SparkContext
import org.apache.spark.util.Vector
import org.apache.spark.rdd.RDD
import org.apache.log4j.PropertyConfigurator
import akka.util.Duration
import org.altic.spark.clustering.utils.DataGenerator
import org.altic.spark.clustering.som.SomTrainerB
import org.altic.spark.clustering.bitm.{BiTM, Croeuc}
import java.util.concurrent.TimeUnit._

/**
 * Company : Altic - LIPN
 * User: Tugdual Sarazin
 * Date: 06/01/14
 * Time: 12:50
 */
object RunAllClustering extends App {
//object RunAllClustering {
  def context() : SparkContext = {
    val prgName = this.getClass.getSimpleName
    if (args.length > 0) {
      // export SPARK_HOME=/home/tug/ScalaProjects/spark-0.8.1-incubating-bin-cdh4
      // export SPARK_RUN_JAR=/home/tug/ScalaProjects/spark-clustering/target/scala-2.9.3/spark-clustering-assembly-0.8.1-SNAPSHOT.jar
      // java -jar /home/tug/ScalaProjects/spark-clustering/target/scala-2.9.3/spark-clustering-assembly-0.8.1-SNAPSHOT.jar spark://localhost.localdomain:7077
      // scp -P 2822 /home/tug/ScalaProjects/spark-clustering/target/scala-2.9.3/spark-clustering-assembly-0.8.1-SNAPSHOT.jar tugdual@magi.univ-paris13.fr:/home/dist/tugdual/runSparkSlurm
      println("## "+args(0)+" ##")

      System.setProperty("SPARK_MEM", "8g")
      System.setProperty("spark.local.dir", "/tmp/spark")

      new SparkContext(args(0), prgName, System.getenv("SPARK_HOME"), Seq(System.getenv("SPARK_RUN_JAR")))
    } else {
      println("## LOCAL ##")
      //PropertyConfigurator.configure("conf/log4j.properties")
      new SparkContext("local", prgName)
    }
  }

  val nbRowSOM = 5
  val nbColSOM = 5
  val nbIter = 3
  val dataNbObs = 400
  //val dataNbObs = 40
  val dataNbVars = 10
  //val dataNbVars = 2

  val sc = context()

  // Data generation
  val arrDatas = DataGenerator.gen2ClsNDims(dataNbObs, dataNbVars).getNamedVector
  //val arrDatas = Array(new NamedVector(Array(1.0, 1.1), 1), new NamedVector(Array(1.5, 1.6), 1), new NamedVector(Array(2.0, 2.1), 2))
  //val arrDatas = Array.tabulate(dataNbObs)(i => new Vector(Array.tabulate(dataNbVars)(j => i*20+j+10)))
  //val arrDatas = Array.tabulate(dataNbObs)(i => new AffectedVector(1, Array.tabulate(dataNbVars)(j => 1)))
  val datas = sc.parallelize(arrDatas, 1)


  println("****************\n**** CROEUC ****\n****************")
  val croeuc = new Croeuc(nbRowSOM * nbColSOM, datas)
  var startLearningTime = System.currentTimeMillis()
  croeuc.training(nbIter)
  val croeucDuration = Duration(System.currentTimeMillis() - startLearningTime, MILLISECONDS)

  println("****************\n***** BITM *****\n****************")
  val bitm = new BiTM(nbRowSOM, nbColSOM, datas)
  startLearningTime = System.currentTimeMillis()
  bitm.training(nbIter)
  val bitmDuration = Duration(System.currentTimeMillis() - startLearningTime, MILLISECONDS)

  println("****************\n***** SOM  *****\n****************")
  val som = new SomTrainerB
  val somOptions = Map("clustering.som.nbrow" -> nbRowSOM.toString, "clustering.som.nbcol" -> nbColSOM.toString)
  val somConvergeDist = -0.1
  startLearningTime = System.currentTimeMillis()
  som.training(datas.asInstanceOf[RDD[Vector]], somOptions, nbIter, somConvergeDist)
  val somDuration = Duration(System.currentTimeMillis() - startLearningTime, MILLISECONDS)

  println("****************\n** Durations  **\n****************")
  println("Croeuc duration:"+croeucDuration)
  println("BiTM duration:"+bitmDuration)
  println("SOM duration:"+somDuration)
}