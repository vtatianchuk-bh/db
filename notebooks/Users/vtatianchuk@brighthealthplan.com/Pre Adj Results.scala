// Databricks notebook source

val path = "dbfs:/mnt/claim-data-preadj/dev/left_loomis/"


createDatabase("dev_bhc_claim_edi_loomis", getFileMap(path),false ,true )

  /**
   * Create unmanaged/external tables for the source/target parquet files
   */
  def createDatabase(databaseName: String, fileMap: scala.collection.mutable.Map[String, String], isPartitioned: Boolean = false, dropExisting: Boolean = false) {
    spark.sql(s"""CREATE DATABASE IF NOT EXISTS """ + databaseName + """ """);

    for (i <- fileMap.keys) {
      val tableName = databaseName + "." + i
      val filePath = fileMap(i)
 println("KEY " + i )
      println("PATH " + fileMap(i) )
      if (dropExisting) {
        spark.sql(s"""DROP TABLE IF EXISTS """ + tableName + """ """)
      }

      spark.sql(s"""CREATE TABLE IF NOT EXISTS """ + tableName + """ USING PARQUET LOCATION '""" + filePath + """' OPTIONS ('compression' = 'gzip') COMMENT '""" + filePath + """'""")

      if (isPartitioned) {
        spark.sql(s"""MSCK REPAIR TABLE """ + tableName + """ """)
      }

      spark.sql(s"""REFRESH TABLE """ + tableName + """ """)
    }
  }

  /**
   * Generates a map for the input files (parquet format)
   */
  def getFileMap(location: String): scala.collection.mutable.Map[String, String] = {
    var map: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map()

    map += "edi_interchange_envelope_group" -> (location + "edi_interchange_envelope_group.gz.parquet")
    map += "edi_transaction_set" -> (location + "edi_transaction_set.gz.parquet")
    map += "edi_transaction_set_info_source" -> (location + "edi_transaction_set_info_source.gz.parquet")
    map += "edi_transaction_set_subscriber" -> (location + "edi_transaction_set_subscriber.gz.parquet")
    map += "edi_transaction_set_patient" -> (location + "edi_transaction_set_patient.gz.parquet")
    map += "edi_transaction_set_claim_date" -> (location + "edi_transaction_set_claim_date.gz.parquet")
    map += "edi_transaction_set_claim_detail" -> (location + "edi_transaction_set_claim_detail.gz.parquet")
    map += "edi_transaction_set_claim_other_payer_provider" -> (location + "edi_transaction_set_claim_other_payer_provider.gz.parquet")
    map += "edi_transaction_set_claim_line_adjudication_detail" -> (location + "edi_transaction_set_claim_line_adjudication_detail.gz.parquet")
    map += "edi_transaction_set_claim" -> (location + "edi_transaction_set_claim.gz.parquet")
    map += "edi_transaction_set_claim_other_subscriber" -> (location + "edi_transaction_set_claim_other_subscriber.gz.parquet")
    map += "edi_transaction_set_claim_line" -> (location + "edi_transaction_set_claim_line.gz.parquet")
    map += "edi_provider_detail" -> (location + "edi_provider_detail.gz.parquet")
 
    map
  }

// COMMAND ----------

// MAGIC %fs
// MAGIC ls /mnt/datalake/dev/source/837_claim_service_lines/Cognizant/history/

// COMMAND ----------



// COMMAND ----------

val path= "dbfs:/mnt/datalake/dev/source/837_claim_service_lines/Cognizant/history/*/"

val filelist=dbutils.fs.ls(path)

val df = filelist.toDF() 
println(df.count())

// COMMAND ----------

val files = spark.sparkContext.wholeTextFiles("dbfs:/mnt/datalake/dev/source/837_claim_service_lines/Cognizant/history/*") 
files.count()

// COMMAND ----------

// MAGIC %sql
// MAGIC select count(*) from dev_bhc_claim_edi_loomis.edi_interchange_envelope_group
// MAGIC --group by claim_number
// MAGIC --where  claim_number="IHS62350810"
// MAGIC --8408563
// MAGIC --

// COMMAND ----------

// MAGIC %sql
// MAGIC select 
// MAGIC * from dev_bhc_claim_edi_loomis.edi_transaction_set_claim_line_adjudication_detail 
// MAGIC --where  Interchange_control_number= '008540231'

// COMMAND ----------

// MAGIC %sql
// MAGIC select service_date_time_period_format_qualifier, service_date,recertification_date,
// MAGIC therapy_begin_date,
// MAGIC last_certification_date,
// MAGIC last_seen_date,
// MAGIC test_date,
// MAGIC shipped_date,
// MAGIC last_xray_date,
// MAGIC initial_treatment_date,
// MAGIC 
// MAGIC 
// MAGIC * from dev_bhc_claim_edi_loomis.edi_transaction_set_claim_line 
// MAGIC where  claim_number like  "%IHS68353997%"

// COMMAND ----------

// MAGIC %sql
// MAGIC select 
// MAGIC functional_date, payer_responsibility_sequence_number_code ,billing_provider_secondary_identification_id, * from dev_claim_edi.edi_transaction_set_subscriber as s
// MAGIC join dev_claim_edi.edi_interchange_envelope_group  as i on i.interchange_control_number = s.interchange_control_number
// MAGIC where i.interchange_control_number = 005814599 -- and s.claim_number = "IHS59330353"
// MAGIC --where payer_responsibility_sequence_number_code is not null.filter($"interchange_control_number" === "005814599")

// COMMAND ----------

val fileNm = dbutils.fs.ls("dbfs:/mnt/datalake/dev/source/837_claim_service_lines/loomis/history/").map(_.name).filter(r => r.startsWith("snapshot_date=2018"))

fileNm.foreach{ f => 
val fileLoc = "dbfs:/mnt/datalake/dev/source/837_claim_service_lines/loomis/history/" + f
dbutils.fs.mv(fileLoc, "dbfs:/mnt/claim-data-preadj/dev/source/loomis/landing/2018", true) }

// COMMAND ----------

 val files = spark.sparkContext.wholeTextFiles("dbfs:/mnt/claim-data-preadj/dev/source/loomis/landing/")


  println(files.count())

//  val oneFileInst = files
//       .filter(f => f._1.split("/")
//         .last.contains("I.X12"))
//       .map(line => line._2)
//       .coalesce(1)
//     val oneFileProf = files
//       .filter(f => f._1.split("/")
//         .last.contains("P.X12"))
//       .map(line => line._2)
//       .coalesce(1)

// oneFileInst.saveAsTextFile("dbfs:/mnt/claim-data-preadj/dev/source_test/loomis/landing/InstClaims")//.saveAsTextFile(configMap("working_location") + "InstClaims")
  


// COMMAND ----------

val edi_interchange_envelope_group = spark.read.parquet("dbfs:/mnt/claim-data-preadj/dev/left/edi_interchange_envelope_group.gz.parquet")
val edi_transaction_set = spark.read.parquet("dbfs:/mnt/claim-data-preadj/dev/left/edi_transaction_set.gz.parquet")
val edi_transaction_set_patient = spark.read.parquet("dbfs:/mnt/claim-data-preadj/dev/left/edi_transaction_set_patient.gz.parquet")
val edi_provider_detail = spark.read.parquet("dbfs:/mnt/claim-data-preadj/dev/left/edi_provider_detail.gz.parquet")
val edi_transaction_set_claim_date = spark.read.parquet("dbfs:/mnt/claim-data-preadj/dev/left/edi_transaction_set_claim_date.gz.parquet")
val edi_transaction_set_claim = spark.read.parquet("dbfs:/mnt/claim-data-preadj/dev/left/edi_transaction_set_claim.gz.parquet")
val edi_transaction_set_claim_detail = spark.read.parquet("dbfs:/mnt/claim-data-preadj/dev/left/edi_transaction_set_claim_detail.gz.parquet")
val edi_transaction_set_subscriber =  spark.read.parquet("dbfs:/mnt/claim-data-preadj/dev/left/edi_transaction_set_subscriber.gz.parquet")

// COMMAND ----------

// MAGIC %md ##Left data model tables

// COMMAND ----------

//display(edi_interchange_envelope_group)//.filter($"interchange_control_number" === "006564757")) 
edi_interchange_envelope_group.count()


// COMMAND ----------

//display(edi_transaction_set.groupBy($"interchange_control_number").count()) 
display(edi_transaction_set.filter($"interchange_control_number" === "005931272")) 

// COMMAND ----------

//919948
display(edi_transaction_set_patient.filter($"interchange_control_number" === "005931272")) 

// COMMAND ----------

display(edi_transaction_set_claim.groupBy($"claim_number").count())//.filter($"interchange_control_number" === "005931272")) //$"interchange_control_number" === "006806290" &&

// COMMAND ----------

 display(edi_transaction_set_claim)//.filter($"claim_number" === "IHS63867101")) 

// COMMAND ----------


display(edi_transaction_set_claim_detail)


// COMMAND ----------

display(edi_transaction_set_subscriber.filter($"claim_number" === "IHS68337349"))

// COMMAND ----------

// DBTITLE 1,Converted to XML Sample file
<loop id="ISA_LOOP">
  <segments>
    <segment id="ISA">
      <elements>
        <element id="ISA01">00</element>
        <element id="ISA02">          </element>
        <element id="ISA03">00</element>
        <element id="ISA04">          </element>
        <element id="ISA05">01</element>
        <element id="ISA06">030240928      </element>
        <element id="ISA07">ZZ</element>
        <element id="ISA08">BRT01          </element>
        <element id="ISA09">200701</element>
        <element id="ISA10">1631</element>
        <element id="ISA11">^</element>
        <element id="ISA12">00501</element>
        <element id="ISA13">TestClaim</element>
        <element id="ISA14">0</element>
        <element id="ISA15">P</element>
        <element id="ISA16">:</element>
      </elements>
    </segment>
    <segment id="IEA">
      <elements>
        <element id="IEA01">1</element>
        <element id="IEA02">382511886</element>
      </elements>
    </segment>
  </segments>
  <loops>
    <loop id="GS_LOOP">
      <segments>
        <segment id="GS">
          <elements>
            <element id="GS01">HC</element>
            <element id="GS02">030240928</element>
            <element id="GS03">BRT01</element>
            <element id="GS04">20200701</element>
            <element id="GS05">1631</element>
            <element id="GS06">701634961</element>
            <element id="GS07">X</element>
            <element id="GS08">005010X223A2</element>
          </elements>
        </segment>
        <segment id="GE">
          <elements>
            <element id="GE01">1</element>
            <element id="GE02">701634961</element>
          </elements>
        </segment>
      </segments>
      <loops>
        <loop id="ST_LOOP">
          <segments>
            <segment id="ST">
              <elements>
                <element id="ST01">837</element>
                <element id="ST02">1001</element>
                <element id="ST03">005010X223A2</element>
              </elements>
            </segment>
            <segment id="SE">
              <elements>
                <element id="SE01">80</element>
                <element id="SE02">1001</element>
              </elements>
            </segment>
          </segments>
          <loops>
            <loop id="HEADER">
              <segments>
                <segment id="BHT">
                  <elements>
                    <element id="BHT01">0019</element>
                    <element id="BHT02">00</element>
                    <element id="BHT03">16246830930</element>
                    <element id="BHT04">20200701</element>
                    <element id="BHT05">1631</element>
                    <element id="BHT06">CH</element>
                  </elements>
                </segment>
              </segments>
              <loops>
                <loop id="1000A">
                  <segments>
                    <segment id="NM1">
                      <elements>
                        <element id="NM101">41</element>
                        <element id="NM102">2</element>
                        <element id="NM103">AVAILITY LLC1</element>
                        <element id="NM104"></element>
                        <element id="NM105"></element>
                        <element id="NM106"></element>
                        <element id="NM107"></element>
                        <element id="NM108">46</element>
                        <element id="NM109">030240928</element>
                      </elements>
                    </segment>
                    <segment id="PER">
                      <elements>
                        <element id="PER01">IC</element>
                        <element id="PER02">AVAILITY CLIENT SERVICES1</element>
                        <element id="PER03">TE</element>
                        <element id="PER04">8002824548</element>
                        <element id="PER05">FX</element>
                        <element id="PER06">9044702187</element>
                      </elements>
                    </segment>
                  </segments>
                  <loops/>
                </loop>
                <loop id="1000B">
                  <segments>
                    <segment id="NM1">
                      <elements>
                        <element id="NM101">40</element>
                        <element id="NM102">2</element>
                        <element id="NM103">Bright Health Plan1</element>
                        <element id="NM104"></element>
                        <element id="NM105"></element>
                        <element id="NM106"></element>
                        <element id="NM107"></element>
                        <element id="NM108">46</element>
                        <element id="NM109">BRT01</element>
                      </elements>
                    </segment>
                  </segments>
                  <loops/>
                </loop>
              </loops>
            </loop>
            <loop id="DETAIL">
              <segments/>
              <loops>
                <loop id="2000A">
                  <segments>
                    <segment id="HL">
                      <elements>
                        <element id="HL01">1</element>
                        <element id="HL02"></element>
                        <element id="HL03">20</element>
                        <element id="HL04">1</element>
                      </elements>
                    </segment>
                    <segment id="PRV">
                      <elements>
                        <element id="PRV01">BI</element>
                        <element id="PRV02">PXC</element>
                        <element id="PRV03">282N00000X</element>
                      </elements>
                    </segment>
                  </segments>
                  <loops>
                    <loop id="2010AA">
                      <segments>
                        <segment id="NM1">
                          <elements>
                            <element id="NM101">85</element>
                            <element id="NM102">2</element>
                            <element id="NM103">MEDICAL CENTER 1</element>
                            <element id="NM104"></element>
                            <element id="NM105"></element>
                            <element id="NM106"></element>
                            <element id="NM107"></element>
                            <element id="NM108">XX</element>
                            <element id="NM109">1111111111</element>
                          </elements>
                        </segment>
                        <segment id="N3">
                          <elements>
                            <element id="N301">1955 W FRYE RD</element>
                          </elements>
                        </segment>
                        <segment id="N4">
                          <elements>
                            <element id="N401">CHANDLER</element>
                            <element id="N402">AZ</element>
                            <element id="N403">852245605</element>
                          </elements>
                        </segment>
                        <segment id="REF">
                          <elements>
                            <element id="REF01">EI</element>
                            <element id="REF02">721561132</element>
                          </elements>
                        </segment>
                        <segment id="PER">
                          <elements>
                            <element id="PER01">IC</element>
                            <element id="PER02">EDI DEPARTMENT</element>
                            <element id="PER03">TE</element>
                            <element id="PER04">7273297800</element>
                          </elements>
                        </segment>
                      </segments>
                      <loops/>
                    </loop>
                    <loop id="2010AB">
                      <segments>
                        <segment id="NM1">
                          <elements>
                            <element id="NM101">87</element>
                            <element id="NM102">2</element>
                          </elements>
                        </segment>
                        <segment id="N3">
                          <elements>
                            <element id="N301">FILE 1</element>
                          </elements>
                        </segment>
                        <segment id="N4">
                          <elements>
                            <element id="N401">LOS ANGELES</element>
                            <element id="N402">CA</element>
                            <element id="N403">900746224</element>
                          </elements>
                        </segment>
                      </segments>
                      <loops/>
                    </loop>
                  </loops>
                </loop>
                <loop id="2000A">
                  <segments>
                    <segment id="HL">
                      <elements>
                        <element id="HL01">2</element>
                        <element id="HL02"></element>
                        <element id="HL03">20</element>
                        <element id="HL04">1</element>
                      </elements>
                    </segment>
                    <segment id="PRV">
                      <elements>
                        <element id="PRV01">BI</element>
                        <element id="PRV02">PXC</element>
                        <element id="PRV03">282N111X</element>
                      </elements>
                    </segment>
                  </segments>
                  <loops>
                    <loop id="2010AA">
                      <segments>
                        <segment id="NM1">
                          <elements>
                            <element id="NM101">85</element>
                            <element id="NM102">2</element>
                            <element id="NM103">MEDICAL CENTER 2</element>
                            <element id="NM104"></element>
                            <element id="NM105"></element>
                            <element id="NM106"></element>
                            <element id="NM107"></element>
                            <element id="NM108">XX</element>
                            <element id="NM109">2222222222</element>
                          </elements>
                        </segment>
                        <segment id="N3">
                          <elements>
                            <element id="N301">1955 W FRYE RD</element>
                          </elements>
                        </segment>
                        <segment id="N4">
                          <elements>
                            <element id="N401">CHANDLER</element>
                            <element id="N402">AZ</element>
                            <element id="N403">852245605</element>
                          </elements>
                        </segment>
                        <segment id="REF">
                          <elements>
                            <element id="REF01">EI</element>
                            <element id="REF02">721561132</element>
                          </elements>
                        </segment>
                        <segment id="PER">
                          <elements>
                            <element id="PER01">IC</element>
                            <element id="PER02">EDI DEPARTMENT</element>
                            <element id="PER03">TE</element>
                            <element id="PER04">7273297800</element>
                          </elements>
                        </segment>
                      </segments>
                      <loops/>
                    </loop>
                    <loop id="2010AB">
                      <segments>
                        <segment id="NM1">
                          <elements>
                            <element id="NM101">87</element>
                            <element id="NM102">2</element>
                          </elements>
                        </segment>
                        <segment id="N3">
                          <elements>
                            <element id="N301">FILE 2</element>
                          </elements>
                        </segment>
                        <segment id="N4">
                          <elements>
                            <element id="N401">LOS ANGELES</element>
                            <element id="N402">CA</element>
                            <element id="N403">900746224</element>
                          </elements>
                        </segment>
                      </segments>
                      <loops/>
                    </loop>
                    <loop id="2000B">
                      <segments>
                        <segment id="HL">
                          <elements>
                            <element id="HL01">3</element>
                            <element id="HL02">1</element>
                            <element id="HL03">22</element>
                            <element id="HL04">0</element>
                          </elements>
                        </segment>
                        <segment id="SBR">
                          <elements>
                            <element id="SBR01">S</element>
                            <element id="SBR02">18</element>
                            <element id="SBR03">H4853002</element>
                            <element id="SBR04"></element>
                            <element id="SBR05"></element>
                            <element id="SBR06"></element>
                            <element id="SBR07"></element>
                            <element id="SBR08"></element>
                            <element id="SBR09">16</element>
                          </elements>
                        </segment>
                      </segments>
                      <loops>
                        <loop id="2010BA">
                          <segments>
                            <segment id="NM1">
                              <elements>
                                <element id="NM101">IL</element>
                                <element id="NM102">1</element>
                                <element id="NM103">CANIGLIA</element>
                                <element id="NM104">JUDITH</element>
                                <element id="NM105"></element>
                                <element id="NM106"></element>
                                <element id="NM107"></element>
                                <element id="NM108">MI</element>
                                <element id="NM109">500001374</element>
                              </elements>
                            </segment>
                            <segment id="N3">
                              <elements>
                                <element id="N301">25435 S WYOMING AVE</element>
                              </elements>
                            </segment>
                            <segment id="N4">
                              <elements>
                                <element id="N401">SUN LAKES</element>
                                <element id="N402">AZ</element>
                                <element id="N403">85248</element>
                              </elements>
                            </segment>
                            <segment id="DMG">
                              <elements>
                                <element id="DMG01">D8</element>
                                <element id="DMG02">19461019</element>
                                <element id="DMG03">F</element>
                              </elements>
                            </segment>
                          </segments>
                          <loops/>
                        </loop>
                        <loop id="2010BB">
                          <segments>
                            <segment id="NM1">
                              <elements>
                                <element id="NM101">PR</element>
                                <element id="NM102">2</element>
                                <element id="NM103">Bright Health Plan</element>
                                <element id="NM104"></element>
                                <element id="NM105"></element>
                                <element id="NM106"></element>
                                <element id="NM107"></element>
                                <element id="NM108">PI</element>
                                <element id="NM109">BRT01</element>
                              </elements>
                            </segment>
                          </segments>
                          <loops/>
                        </loop>
                        <loop id="2300">
                          <segments>
                            <segment id="CLM">
                              <elements>
                                <element id="CLM01">84011625286S2C0000</element>
                                <element id="CLM02">1821</element>
                                <element id="CLM03"></element>
                                <element id="CLM04"></element>
                                <element id="CLM05">13:A:1</element>
                                <element id="CLM06"></element>
                                <element id="CLM07">A</element>
                                <element id="CLM08">Y</element>
                                <element id="CLM09">Y</element>
                              </elements>
                            </segment>
                            <segment id="DTP">
                              <elements>
                                <element id="DTP01">400</element>
                                <element id="DTP02">RD8</element>
                                <element id="DTP03">20200124-20200124</element>
                              </elements>
                            </segment>
                            <segment id="CL1">
                              <elements>
                                <element id="CL101">3</element>
                                <element id="CL102">2</element>
                                <element id="CL103">01</element>
                              </elements>
                            </segment>
                            <segment id="REF">
                              <elements>
                                <element id="REF01">D9</element>
                                <element id="REF02">16246830930</element>
                              </elements>
                            </segment>
                            <segment id="EF">
                              <elements>
                                <element id="EF01">EA</element>
                                <element id="EF02">3502125175</element>
                              </elements>
                            </segment>
                            <segment id="NTE">
                              <elements>
                                <element id="NTE01">UPI</element>
                                <element id="NTE02">HOSPICE PERIOD REVOKED      MB 061520</element>
                              </elements>
                            </segment>
                            <segment id="HI">
                              <elements>
                                <element id="HI01">ABK:C3431</element>
                              </elements>
                            </segment>
                            <segment id="HI">
                              <elements>
                                <element id="HI01">APR:C3431</element>
                              </elements>
                            </segment>
                            <segment id="HI">
                              <elements>
                                <element id="HI01">BH:11:D8:20200124</element>
                              </elements>
                            </segment>
                            <segment id="HI">
                              <elements>
                                <element id="HI01">BG:07</element>
                              </elements>
                            </segment>
                          </segments>
                          <loops>
                            <loop id="2310A">
                              <segments>
                                <segment id="NM1">
                                  <elements>
                                    <element id="NM101">71</element>
                                    <element id="NM102">1</element>
                                    <element id="NM103">DEROOCK</element>
                                    <element id="NM104">IAN</element>
                                    <element id="NM105">B</element>
                                    <element id="NM106"></element>
                                    <element id="NM107"></element>
                                    <element id="NM108">XX</element>
                                    <element id="NM109">1114900479</element>
                                  </elements>
                                </segment>
                              </segments>
                              <loops/>
                            </loop>
                            <loop id="2320">
                              <segments>
                                <segment id="SBR">
                                  <elements>
                                    <element id="SBR01">P</element>
                                    <element id="SBR02">18</element>
                                    <element id="SBR03"></element>
                                    <element id="SBR04"></element>
                                    <element id="SBR05"></element>
                                    <element id="SBR06"></element>
                                    <element id="SBR07"></element>
                                    <element id="SBR08"></element>
                                    <element id="SBR09">MA</element>
                                  </elements>
                                </segment>
                                <segment id="AMT">
                                  <elements>
                                    <element id="AMT01">D</element>
                                    <element id="AMT02">176.63</element>
                                  </elements>
                                </segment>
                                <segment id="OI">
                                  <elements>
                                    <element id="OI01"></element>
                                    <element id="OI02"></element>
                                    <element id="OI03">Y</element>
                                    <element id="OI04"></element>
                                    <element id="OI05"></element>
                                    <element id="OI06">Y</element>
                                  </elements>
                                </segment>
                              </segments>
                              <loops>
                                <loop id="2330A">
                                  <segments>
                                    <segment id="NM1">
                                      <elements>
                                        <element id="NM101">IL</element>
                                        <element id="NM102">1</element>
                                        <element id="NM103">CANIGLIA</element>
                                        <element id="NM104">JUDITH</element>
                                        <element id="NM105"></element>
                                        <element id="NM106"></element>
                                        <element id="NM107"></element>
                                        <element id="NM108">MI</element>
                                        <element id="NM109">3QJ4YU9PK93</element>
                                      </elements>
                                    </segment>
                                    <segment id="N3">
                                      <elements>
                                        <element id="N301">25435 S WYOMING AVE</element>
                                      </elements>
                                    </segment>
                                    <segment id="N4">
                                      <elements>
                                        <element id="N401">SUN LAKES</element>
                                        <element id="N402">AZ</element>
                                        <element id="N403">85248</element>
                                      </elements>
                                    </segment>
                                  </segments>
                                  <loops/>
                                </loop>
                                <loop id="2330B">
                                  <segments>
                                    <segment id="NM1">
                                      <elements>
                                        <element id="NM101">PR</element>
                                        <element id="NM102">2</element>
                                        <element id="NM103">MEDICARE</element>
                                        <element id="NM104"></element>
                                        <element id="NM105"></element>
                                        <element id="NM106"></element>
                                        <element id="NM107"></element>
                                        <element id="NM108">PI</element>
                                        <element id="NM109">03101</element>
                                      </elements>
                                    </segment>
                                    <segment id="N3">
                                      <elements>
                                        <element id="N301">PO BOX 6770</element>
                                      </elements>
                                    </segment>
                                    <segment id="N4">
                                      <elements>
                                        <element id="N401">FARGO</element>
                                        <element id="N402">ND</element>
                                        <element id="N403">581086770</element>
                                      </elements>
                                    </segment>
                                    <segment id="REF">
                                      <elements>
                                        <element id="REF01">F8</element>
                                        <element id="REF02">22016700578404AZA</element>
                                      </elements>
                                    </segment>
                                  </segments>
                                  <loops/>
                                </loop>
                              </loops>
                            </loop>
                            <loop id="2400">
                              <segments>
                                <segment id="LX">
                                  <elements>
                                    <element id="LX01">1</element>
                                  </elements>
                                </segment>
                                <segment id="SV2">
                                  <elements>
                                    <element id="SV201">0260</element>
                                    <element id="SV202">HC:96360:::::IV THERAPY</element>
                                    <element id="SV203">675</element>
                                    <element id="SV204">UN</element>
                                    <element id="SV205">1</element>
                                  </elements>
                                </segment>
                                <segment id="DTP">
                                  <elements>
                                    <element id="DTP01">472</element>
                                    <element id="DTP02">D8</element>
                                    <element id="DTP03">20200124</element>
                                  </elements>
                                </segment>
                                <segment id="REF">
                                  <elements>
                                    <element id="REF01">6R</element>
                                    <element id="REF02">A458-1064441-00001</element>
                                  </elements>
                                </segment>
                              </segments>
                              <loops>
                                <loop id="2430">
                                  <segments>
                                    <segment id="SVD">
                                      <elements>
                                        <element id="SVD01">03101</element>
                                        <element id="SVD02">146.3</element>
                                        <element id="SVD03">HC:96360</element>
                                        <element id="SVD04">0260</element>
                                        <element id="SVD05">1</element>
                                      </elements>
                                    </segment>
                                    <segment id="CAS">
                                      <elements>
                                        <element id="CAS01">CO</element>
                                        <element id="CAS02">45</element>
                                        <element id="CAS03">488.39</element>
                                        <element id="CAS04">1</element>
                                        <element id="CAS05">253</element>
                                        <element id="CAS06">2.99</element>
                                        <element id="CAS07">1</element>
                                      </elements>
                                    </segment>
                                    <segment id="CAS">
                                      <elements>
                                        <element id="CAS01">PR</element>
                                        <element id="CAS02">2</element>
                                        <element id="CAS03">37.32</element>
                                        <element id="CAS04">1</element>
                                      </elements>
                                    </segment>
                                    <segment id="DTP">
                                      <elements>
                                        <element id="DTP01">573</element>
                                        <element id="DTP02">D8</element>
                                        <element id="DTP03">20200630</element>
                                      </elements>
                                    </segment>
                                    <segment id="AMT">
                                      <elements>
                                        <element id="AMT01">EAF</element>
                                        <element id="AMT02">37.32</element>
                                      </elements>
                                    </segment>
                                  </segments>
                                  <loops/>
                                </loop>
                              </loops>
                            </loop>
                            <loop id="2400">
                              <segments>
                                <segment id="LX">
                                  <elements>
                                    <element id="LX01">2</element>
                                  </elements>
                                </segment>
                                <segment id="SV2">
                                  <elements>
                                    <element id="SV201">0260</element>
                                    <element id="SV202">HC:96361:::::IV THERAPY</element>
                                    <element id="SV203">774</element>
                                    <element id="SV204">UN</element>
                                    <element id="SV205">1</element>
                                  </elements>
                                </segment>
                                <segment id="DTP">
                                  <elements>
                                    <element id="DTP01">472</element>
                                    <element id="DTP02">D8</element>
                                    <element id="DTP03">20200124</element>
                                  </elements>
                                </segment>
                                <segment id="REF">
                                  <elements>
                                    <element id="REF01">6R</element>
                                    <element id="REF02">A458-1064441-00002</element>
                                  </elements>
                                </segment>
                              </segments>
                              <loops>
                                <loop id="2430">
                                  <segments>
                                    <segment id="SVD">
                                      <elements>
                                        <element id="SVD01">03101</element>
                                        <element id="SVD02">30.33</element>
                                        <element id="SVD03">HC:96361</element>
                                        <element id="SVD04">0260</element>
                                        <element id="SVD05">1</element>
                                      </elements>
                                    </segment>
                                    <segment id="CAS">
                                      <elements>
                                        <element id="CAS01">CO</element>
                                        <element id="CAS02">45</element>
                                        <element id="CAS03">735.3</element>
                                        <element id="CAS04">1</element>
                                        <element id="CAS05">253</element>
                                        <element id="CAS06">.62</element>
                                        <element id="CAS07">1</element>
                                      </elements>
                                    </segment>
                                    <segment id="CAS">
                                      <elements>
                                        <element id="CAS01">PR</element>
                                        <element id="CAS02">2</element>
                                        <element id="CAS03">7.75</element>
                                        <element id="CAS04">1</element>
                                      </elements>
                                    </segment>
                                    <segment id="DTP">
                                      <elements>
                                        <element id="DTP01">573</element>
                                        <element id="DTP02">D8</element>
                                        <element id="DTP03">20200630</element>
                                      </elements>
                                    </segment>
                                    <segment id="AMT">
                                      <elements>
                                        <element id="AMT01">EAF</element>
                                        <element id="AMT02">7.75</element>
                                      </elements>
                                    </segment>
                                  </segments>
                                  <loops/>
                                </loop>
                              </loops>
                            </loop>
                            <loop id="2400">
                              <segments>
                                <segment id="LX">
                                  <elements>
                                    <element id="LX01">3</element>
                                  </elements>
                                </segment>
                                <segment id="SV2">
                                  <elements>
                                    <element id="SV201">0636</element>
                                    <element id="SV202">HC:J3490:::::NACL 09 10ML INJ</element>
                                    <element id="SV203">22</element>
                                    <element id="SV204">UN</element>
                                    <element id="SV205">2</element>
                                  </elements>
                                </segment>
                                <segment id="DTP">
                                  <elements>
                                    <element id="DTP01">472</element>
                                    <element id="DTP02">D8</element>
                                    <element id="DTP03">20200124</element>
                                  </elements>
                                </segment>
                                <segment id="REF">
                                  <elements>
                                    <element id="REF01">6R</element>
                                    <element id="REF02">A458-1064441-00003</element>
                                  </elements>
                                </segment>
                              </segments>
                              <loops>
                                <loop id="2410">
                                  <segments>
                                    <segment id="LIN">
                                      <elements>
                                        <element id="LIN01"></element>
                                        <element id="LIN02">N4</element>
                                        <element id="LIN03">00409488810</element>
                                      </elements>
                                    </segment>
                                    <segment id="CTP">
                                      <elements>
                                        <element id="CTP01"></element>
                                        <element id="CTP02"></element>
                                        <element id="CTP03"></element>
                                        <element id="CTP04">10</element>
                                        <element id="CTP05">ML</element>
                                      </elements>
                                    </segment>
                                  </segments>
                                  <loops/>
                                </loop>
                                <loop id="2430">
                                  <segments>
                                    <segment id="SVD">
                                      <elements>
                                        <element id="SVD01">03101</element>
                                        <element id="SVD02">0</element>
                                        <element id="SVD03">HC:J3490</element>
                                        <element id="SVD04">0636</element>
                                        <element id="SVD05">2</element>
                                      </elements>
                                    </segment>
                                    <segment id="CAS">
                                      <elements>
                                        <element id="CAS01">CO</element>
                                        <element id="CAS02">97</element>
                                        <element id="CAS03">22</element>
                                        <element id="CAS04">1</element>
                                      </elements>
                                    </segment>
                                    <segment id="DTP">
                                      <elements>
                                        <element id="DTP01">573</element>
                                        <element id="DTP02">D8</element>
                                        <element id="DTP03">20200630</element>
                                      </elements>
                                    </segment>
                                  </segments>
                                  <loops/>
                                </loop>
                              </loops>
                            </loop>
                            <loop id="2400">
                              <segments>
                                <segment id="LX">
                                  <elements>
                                    <element id="LX01">4</element>
                                  </elements>
                                </segment>
                                <segment id="SV2">
                                  <elements>
                                    <element id="SV201">0636</element>
                                    <element id="SV202">HC:J7030:::::PHARMACY</element>
                                    <element id="SV203">350</element>
                                    <element id="SV204">UN</element>
                                    <element id="SV205">1</element>
                                  </elements>
                                </segment>
                                <segment id="DTP">
                                  <elements>
                                    <element id="DTP01">472</element>
                                    <element id="DTP02">D8</element>
                                    <element id="DTP03">20200124</element>
                                  </elements>
                                </segment>
                                <segment id="REF">
                                  <elements>
                                    <element id="REF01">6R</element>
                                    <element id="REF02">A458-1064441-00004</element>
                                  </elements>
                                </segment>
                              </segments>
                              <loops>
                                <loop id="2410">
                                  <segments>
                                    <segment id="LIN">
                                      <elements>
                                        <element id="LIN01"></element>
                                        <element id="LIN02">N4</element>
                                        <element id="LIN03">00338004904</element>
                                      </elements>
                                    </segment>
                                    <segment id="CTP">
                                      <elements>
                                        <element id="CTP01"></element>
                                        <element id="CTP02"></element>
                                        <element id="CTP03"></element>
                                        <element id="CTP04">1000</element>
                                        <element id="CTP05">ML</element>
                                      </elements>
                                    </segment>
                                  </segments>
                                  <loops/>
                                </loop>
                                <loop id="2430">
                                  <segments>
                                    <segment id="SVD">
                                      <elements>
                                        <element id="SVD01">03101</element>
                                        <element id="SVD02">0</element>
                                        <element id="SVD03">HC:J7030</element>
                                        <element id="SVD04">0636</element>
                                        <element id="SVD05">1</element>
                                      </elements>
                                    </segment>
                                    <segment id="CAS">
                                      <elements>
                                        <element id="CAS01">CO</element>
                                        <element id="CAS02">97</element>
                                        <element id="CAS03">350</element>
                                        <element id="CAS04">1</element>
                                      </elements>
                                    </segment>
                                    <segment id="DTP">
                                      <elements>
                                        <element id="DTP01">573</element>
                                        <element id="DTP02">D8</element>
                                        <element id="DTP03">20200630</element>
                                      </elements>
                                    </segment>
                                  </segments>
                                  <loops/>
                                </loop>
                              </loops>
                            </loop>
                          </loops>
                        </loop>
                      </loops>
                    </loop>
                  </loops>
                </loop>
              </loops>
            </loop>
          </loops>
        </loop>
        <loop id="ST_LOOP">
          <segments>
            <segment id="ST">
              <elements>
                <element id="ST01">837</element>
                <element id="ST02">1002</element>
                <element id="ST03">005010X223A2</element>
              </elements>
            </segment>
            <segment id="SE">
              <elements>
                <element id="SE01">80</element>
                <element id="SE02">1002</element>
              </elements>
            </segment>
          </segments>
          <loops>
            <loop id="HEADER">
              <segments>
                <segment id="BHT">
                  <elements>
                    <element id="BHT01">0019</element>
                    <element id="BHT02">00</element>
                    <element id="BHT03">16246830930</element>
                    <element id="BHT04">20200701</element>
                    <element id="BHT05">1631</element>
                    <element id="BHT06">CH</element>
                  </elements>
                </segment>
              </segments>
              <loops>
                <loop id="1000A">
                  <segments>
                    <segment id="NM1">
                      <elements>
                        <element id="NM101">41</element>
                        <element id="NM102">2</element>
                        <element id="NM103">AVAILITY LLC2</element>
                        <element id="NM104"></element>
                        <element id="NM105"></element>
                        <element id="NM106"></element>
                        <element id="NM107"></element>
                        <element id="NM108">46</element>
                        <element id="NM109">030240928</element>
                      </elements>
                    </segment>
                    <segment id="PER">
                      <elements>
                        <element id="PER01">IC</element>
                        <element id="PER02">AVAILITY CLIENT SERVICES2</element>
                        <element id="PER03">TE</element>
                        <element id="PER04">8002824548</element>
                        <element id="PER05">FX</element>
                        <element id="PER06">9044702187</element>
                      </elements>
                    </segment>
                  </segments>
                  <loops/>
                </loop>
                <loop id="1000B">
                  <segments>
                    <segment id="NM1">
                      <elements>
                        <element id="NM101">40</element>
                        <element id="NM102">2</element>
                        <element id="NM103">Bright Health Plan2</element>
                        <element id="NM104"></element>
                        <element id="NM105"></element>
                        <element id="NM106"></element>
                        <element id="NM107"></element>
                        <element id="NM108">46</element>
                        <element id="NM109">BRT01</element>
                      </elements>
                    </segment>
                  </segments>
                  <loops/>
                </loop>
              </loops>
            </loop>
            <loop id="DETAIL">
              <segments/>
              <loops>
                <loop id="2000A">
                  <segments>
                    <segment id="HL">
                      <elements>
                        <element id="HL01">3</element>
                        <element id="HL02"></element>
                        <element id="HL03">20</element>
                        <element id="HL04">1</element>
                      </elements>
                    </segment>
                    <segment id="PRV">
                      <elements>
                        <element id="PRV01">BI</element>
                        <element id="PRV02">PXC</element>
                        <element id="PRV03">282N222X</element>
                      </elements>
                    </segment>
                  </segments>
                  <loops>
                    <loop id="2010AA">
                      <segments>
                        <segment id="NM1">
                          <elements>
                            <element id="NM101">85</element>
                            <element id="NM102">2</element>
                            <element id="NM103">MEDICAL CENTER 3</element>
                            <element id="NM104"></element>
                            <element id="NM105"></element>
                            <element id="NM106"></element>
                            <element id="NM107"></element>
                            <element id="NM108">XX</element>
                            <element id="NM109">3333333333</element>
                          </elements>
                        </segment>
                        <segment id="N3">
                          <elements>
                            <element id="N301">1955 W FRYE RD</element>
                          </elements>
                        </segment>
                        <segment id="N4">
                          <elements>
                            <element id="N401">CHANDLER</element>
                            <element id="N402">AZ</element>
                            <element id="N403">852245605</element>
                          </elements>
                        </segment>
                        <segment id="REF">
                          <elements>
                            <element id="REF01">EI</element>
                            <element id="REF02">721561132</element>
                          </elements>
                        </segment>
                        <segment id="PER">
                          <elements>
                            <element id="PER01">IC</element>
                            <element id="PER02">EDI DEPARTMENT</element>
                            <element id="PER03">TE</element>
                            <element id="PER04">7273297800</element>
                          </elements>
                        </segment>
                      </segments>
                      <loops/>
                    </loop>
                    <loop id="2010AB">
                      <segments>
                        <segment id="NM1">
                          <elements>
                            <element id="NM101">87</element>
                            <element id="NM102">2</element>
                          </elements>
                        </segment>
                        <segment id="N3">
                          <elements>
                            <element id="N301">FILE 3</element>
                          </elements>
                        </segment>
                        <segment id="N4">
                          <elements>
                            <element id="N401">LOS ANGELES</element>
                            <element id="N402">CA</element>
                            <element id="N403">900746224</element>
                          </elements>
                        </segment>
                      </segments>
                      <loops/>
                    </loop>
                    <loop id="2000B">
                      <segments>
                        <segment id="HL">
                          <elements>
                            <element id="HL01">2</element>
                            <element id="HL02">1</element>
                            <element id="HL03">22</element>
                            <element id="HL04">0</element>
                          </elements>
                        </segment>
                        <segment id="SBR">
                          <elements>
                            <element id="SBR01">S</element>
                            <element id="SBR02">18</element>
                            <element id="SBR03">H4853002</element>
                            <element id="SBR04"></element>
                            <element id="SBR05"></element>
                            <element id="SBR06"></element>
                            <element id="SBR07"></element>
                            <element id="SBR08"></element>
                            <element id="SBR09">16</element>
                          </elements>
                        </segment>
                      </segments>
                      <loops>
                        <loop id="2010BA">
                          <segments>
                            <segment id="NM1">
                              <elements>
                                <element id="NM101">IL</element>
                                <element id="NM102">1</element>
                                <element id="NM103">CANIGLIA</element>
                                <element id="NM104">JUDITH</element>
                                <element id="NM105"></element>
                                <element id="NM106"></element>
                                <element id="NM107"></element>
                                <element id="NM108">MI</element>
                                <element id="NM109">500001374</element>
                              </elements>
                            </segment>
                            <segment id="N3">
                              <elements>
                                <element id="N301">25435 S WYOMING AVE</element>
                              </elements>
                            </segment>
                            <segment id="N4">
                              <elements>
                                <element id="N401">SUN LAKES</element>
                                <element id="N402">AZ</element>
                                <element id="N403">85248</element>
                              </elements>
                            </segment>
                            <segment id="DMG">
                              <elements>
                                <element id="DMG01">D8</element>
                                <element id="DMG02">19461019</element>
                                <element id="DMG03">F</element>
                              </elements>
                            </segment>
                          </segments>
                          <loops/>
                        </loop>
                        <loop id="2010BB">
                          <segments>
                            <segment id="NM1">
                              <elements>
                                <element id="NM101">PR</element>
                                <element id="NM102">2</element>
                                <element id="NM103">Bright Health Plan</element>
                                <element id="NM104"></element>
                                <element id="NM105"></element>
                                <element id="NM106"></element>
                                <element id="NM107"></element>
                                <element id="NM108">PI</element>
                                <element id="NM109">BRT01</element>
                              </elements>
                            </segment>
                          </segments>
                          <loops/>
                        </loop>
                        <loop id="2300">
                          <segments>
                            <segment id="CLM">
                              <elements>
                                <element id="CLM01">84011625286S2C1111</element>
                                <element id="CLM02">1821</element>
                                <element id="CLM03"></element>
                                <element id="CLM04"></element>
                                <element id="CLM05">13:A:1</element>
                                <element id="CLM06"></element>
                                <element id="CLM07">A</element>
                                <element id="CLM08">Y</element>
                                <element id="CLM09">Y</element>
                              </elements>
                            </segment>
                            <segment id="DTP">
                              <elements>
                                <element id="DTP01">434</element>
                                <element id="DTP02">RD8</element>
                                <element id="DTP03">20200124-20200124</element>
                              </elements>
                            </segment>
                            <segment id="CL1">
                              <elements>
                                <element id="CL101">3</element>
                                <element id="CL102">2</element>
                                <element id="CL103">01</element>
                              </elements>
                            </segment>
                            <segment id="REF">
                              <elements>
                                <element id="REF01">D9</element>
                                <element id="REF02">16246830930</element>
                              </elements>
                            </segment>
                            <segment id="EF">
                              <elements>
                                <element id="EF01">EA</element>
                                <element id="EF02">3502125175</element>
                              </elements>
                            </segment>
                            <segment id="NTE">
                              <elements>
                                <element id="NTE01">UPI</element>
                                <element id="NTE02">HOSPICE PERIOD REVOKED      MB 061520</element>
                              </elements>
                            </segment>
                            <segment id="HI">
                              <elements>
                                <element id="HI01">ABK:C3431</element>
                              </elements>
                            </segment>
                            <segment id="HI">
                              <elements>
                                <element id="HI01">APR:C3431</element>
                              </elements>
                            </segment>
                            <segment id="HI">
                              <elements>
                                <element id="HI01">BH:11:D8:20200124</element>
                              </elements>
                            </segment>
                            <segment id="HI">
                              <elements>
                                <element id="HI01">BG:07</element>
                              </elements>
                            </segment>
                          </segments>
                          <loops>
                            <loop id="2310A">
                              <segments>
                                <segment id="NM1">
                                  <elements>
                                    <element id="NM101">71</element>
                                    <element id="NM102">1</element>
                                    <element id="NM103">DEROOCK</element>
                                    <element id="NM104">IAN</element>
                                    <element id="NM105">B</element>
                                    <element id="NM106"></element>
                                    <element id="NM107"></element>
                                    <element id="NM108">XX</element>
                                    <element id="NM109">1114900479</element>
                                  </elements>
                                </segment>
                              </segments>
                              <loops/>
                            </loop>
                            <loop id="2320">
                              <segments>
                                <segment id="SBR">
                                  <elements>
                                    <element id="SBR01">P</element>
                                    <element id="SBR02">18</element>
                                    <element id="SBR03"></element>
                                    <element id="SBR04"></element>
                                    <element id="SBR05"></element>
                                    <element id="SBR06"></element>
                                    <element id="SBR07"></element>
                                    <element id="SBR08"></element>
                                    <element id="SBR09">MA</element>
                                  </elements>
                                </segment>
                                <segment id="AMT">
                                  <elements>
                                    <element id="AMT01">D</element>
                                    <element id="AMT02">176.63</element>
                                  </elements>
                                </segment>
                                <segment id="OI">
                                  <elements>
                                    <element id="OI01"></element>
                                    <element id="OI02"></element>
                                    <element id="OI03">Y</element>
                                    <element id="OI04"></element>
                                    <element id="OI05"></element>
                                    <element id="OI06">Y</element>
                                  </elements>
                                </segment>
                              </segments>
                              <loops>
                                <loop id="2330A">
                                  <segments>
                                    <segment id="NM1">
                                      <elements>
                                        <element id="NM101">IL</element>
                                        <element id="NM102">1</element>
                                        <element id="NM103">CANIGLIA</element>
                                        <element id="NM104">JUDITH</element>
                                        <element id="NM105"></element>
                                        <element id="NM106"></element>
                                        <element id="NM107"></element>
                                        <element id="NM108">MI</element>
                                        <element id="NM109">3QJ4YU9PK93</element>
                                      </elements>
                                    </segment>
                                    <segment id="N3">
                                      <elements>
                                        <element id="N301">25435 S WYOMING AVE</element>
                                      </elements>
                                    </segment>
                                    <segment id="N4">
                                      <elements>
                                        <element id="N401">SUN LAKES</element>
                                        <element id="N402">AZ</element>
                                        <element id="N403">85248</element>
                                      </elements>
                                    </segment>
                                  </segments>
                                  <loops/>
                                </loop>
                                <loop id="2330B">
                                  <segments>
                                    <segment id="NM1">
                                      <elements>
                                        <element id="NM101">PR</element>
                                        <element id="NM102">2</element>
                                        <element id="NM103">MEDICARE</element>
                                        <element id="NM104"></element>
                                        <element id="NM105"></element>
                                        <element id="NM106"></element>
                                        <element id="NM107"></element>
                                        <element id="NM108">PI</element>
                                        <element id="NM109">03101</element>
                                      </elements>
                                    </segment>
                                    <segment id="N3">
                                      <elements>
                                        <element id="N301">PO BOX 6770</element>
                                      </elements>
                                    </segment>
                                    <segment id="N4">
                                      <elements>
                                        <element id="N401">FARGO</element>
                                        <element id="N402">ND</element>
                                        <element id="N403">581086770</element>
                                      </elements>
                                    </segment>
                                    <segment id="REF">
                                      <elements>
                                        <element id="REF01">F8</element>
                                        <element id="REF02">22016700578404AZA</element>
                                      </elements>
                                    </segment>
                                  </segments>
                                  <loops/>
                                </loop>
                              </loops>
                            </loop>
                            <loop id="2400">
                              <segments>
                                <segment id="LX">
                                  <elements>
                                    <element id="LX01">1</element>
                                  </elements>
                                </segment>
                                <segment id="SV2">
                                  <elements>
                                    <element id="SV201">0260</element>
                                    <element id="SV202">HC:96360:::::IV THERAPY</element>
                                    <element id="SV203">675</element>
                                    <element id="SV204">UN</element>
                                    <element id="SV205">1</element>
                                  </elements>
                                </segment>
                                <segment id="DTP">
                                  <elements>
                                    <element id="DTP01">472</element>
                                    <element id="DTP02">D8</element>
                                    <element id="DTP03">20200124</element>
                                  </elements>
                                </segment>
                                <segment id="REF">
                                  <elements>
                                    <element id="REF01">6R</element>
                                    <element id="REF02">A458-1064441-00001</element>
                                  </elements>
                                </segment>
                              </segments>
                              <loops>
                                <loop id="2430">
                                  <segments>
                                    <segment id="SVD">
                                      <elements>
                                        <element id="SVD01">03101</element>
                                        <element id="SVD02">146.3</element>
                                        <element id="SVD03">HC:96360</element>
                                        <element id="SVD04">0260</element>
                                        <element id="SVD05">1</element>
                                      </elements>
                                    </segment>
                                    <segment id="CAS">
                                      <elements>
                                        <element id="CAS01">CO</element>
                                        <element id="CAS02">45</element>
                                        <element id="CAS03">488.39</element>
                                        <element id="CAS04">1</element>
                                        <element id="CAS05">253</element>
                                        <element id="CAS06">2.99</element>
                                        <element id="CAS07">1</element>
                                      </elements>
                                    </segment>
                                    <segment id="CAS">
                                      <elements>
                                        <element id="CAS01">PR</element>
                                        <element id="CAS02">2</element>
                                        <element id="CAS03">37.32</element>
                                        <element id="CAS04">1</element>
                                      </elements>
                                    </segment>
                                    <segment id="DTP">
                                      <elements>
                                        <element id="DTP01">573</element>
                                        <element id="DTP02">D8</element>
                                        <element id="DTP03">20200630</element>
                                      </elements>
                                    </segment>
                                    <segment id="AMT">
                                      <elements>
                                        <element id="AMT01">EAF</element>
                                        <element id="AMT02">37.32</element>
                                      </elements>
                                    </segment>
                                  </segments>
                                  <loops/>
                                </loop>
                              </loops>
                            </loop>
                            <loop id="2400">
                              <segments>
                                <segment id="LX">
                                  <elements>
                                    <element id="LX01">2</element>
                                  </elements>
                                </segment>
                                <segment id="SV2">
                                  <elements>
                                    <element id="SV201">0260</element>
                                    <element id="SV202">HC:96361:::::IV THERAPY</element>
                                    <element id="SV203">774</element>
                                    <element id="SV204">UN</element>
                                    <element id="SV205">1</element>
                                  </elements>
                                </segment>
                                <segment id="DTP">
                                  <elements>
                                    <element id="DTP01">472</element>
                                    <element id="DTP02">D8</element>
                                    <element id="DTP03">20200124</element>
                                  </elements>
                                </segment>
                                <segment id="REF">
                                  <elements>
                                    <element id="REF01">6R</element>
                                    <element id="REF02">A458-1064441-00002</element>
                                  </elements>
                                </segment>
                              </segments>
                              <loops>
                                <loop id="2430">
                                  <segments>
                                    <segment id="SVD">
                                      <elements>
                                        <element id="SVD01">03101</element>
                                        <element id="SVD02">30.33</element>
                                        <element id="SVD03">HC:96361</element>
                                        <element id="SVD04">0260</element>
                                        <element id="SVD05">1</element>
                                      </elements>
                                    </segment>
                                    <segment id="CAS">
                                      <elements>
                                        <element id="CAS01">CO</element>
                                        <element id="CAS02">45</element>
                                        <element id="CAS03">735.3</element>
                                        <element id="CAS04">1</element>
                                        <element id="CAS05">253</element>
                                        <element id="CAS06">.62</element>
                                        <element id="CAS07">1</element>
                                      </elements>
                                    </segment>
                                    <segment id="CAS">
                                      <elements>
                                        <element id="CAS01">PR</element>
                                        <element id="CAS02">2</element>
                                        <element id="CAS03">7.75</element>
                                        <element id="CAS04">1</element>
                                      </elements>
                                    </segment>
                                    <segment id="DTP">
                                      <elements>
                                        <element id="DTP01">573</element>
                                        <element id="DTP02">D8</element>
                                        <element id="DTP03">20200630</element>
                                      </elements>
                                    </segment>
                                    <segment id="AMT">
                                      <elements>
                                        <element id="AMT01">EAF</element>
                                        <element id="AMT02">7.75</element>
                                      </elements>
                                    </segment>
                                  </segments>
                                  <loops/>
                                </loop>
                              </loops>
                            </loop>
                            <loop id="2400">
                              <segments>
                                <segment id="LX">
                                  <elements>
                                    <element id="LX01">3</element>
                                  </elements>
                                </segment>
                                <segment id="SV2">
                                  <elements>
                                    <element id="SV201">0636</element>
                                    <element id="SV202">HC:J3490:::::NACL 09 10ML INJ</element>
                                    <element id="SV203">22</element>
                                    <element id="SV204">UN</element>
                                    <element id="SV205">2</element>
                                  </elements>
                                </segment>
                                <segment id="DTP">
                                  <elements>
                                    <element id="DTP01">472</element>
                                    <element id="DTP02">D8</element>
                                    <element id="DTP03">20200124</element>
                                  </elements>
                                </segment>
                                <segment id="REF">
                                  <elements>
                                    <element id="REF01">6R</element>
                                    <element id="REF02">A458-1064441-00003</element>
                                  </elements>
                                </segment>
                              </segments>
                              <loops>
                                <loop id="2410">
                                  <segments>
                                    <segment id="LIN">
                                      <elements>
                                        <element id="LIN01"></element>
                                        <element id="LIN02">N4</element>
                                        <element id="LIN03">00409488810</element>
                                      </elements>
                                    </segment>
                                    <segment id="CTP">
                                      <elements>
                                        <element id="CTP01"></element>
                                        <element id="CTP02"></element>
                                        <element id="CTP03"></element>
                                        <element id="CTP04">10</element>
                                        <element id="CTP05">ML</element>
                                      </elements>
                                    </segment>
                                  </segments>
                                  <loops/>
                                </loop>
                                <loop id="2430">
                                  <segments>
                                    <segment id="SVD">
                                      <elements>
                                        <element id="SVD01">03101</element>
                                        <element id="SVD02">0</element>
                                        <element id="SVD03">HC:J3490</element>
                                        <element id="SVD04">0636</element>
                                        <element id="SVD05">2</element>
                                      </elements>
                                    </segment>
                                    <segment id="CAS">
                                      <elements>
                                        <element id="CAS01">CO</element>
                                        <element id="CAS02">97</element>
                                        <element id="CAS03">22</element>
                                        <element id="CAS04">1</element>
                                      </elements>
                                    </segment>
                                    <segment id="DTP">
                                      <elements>
                                        <element id="DTP01">573</element>
                                        <element id="DTP02">D8</element>
                                        <element id="DTP03">20200630</element>
                                      </elements>
                                    </segment>
                                  </segments>
                                  <loops/>
                                </loop>
                              </loops>
                            </loop>
                            <loop id="2400">
                              <segments>
                                <segment id="LX">
                                  <elements>
                                    <element id="LX01">4</element>
                                  </elements>
                                </segment>
                                <segment id="SV2">
                                  <elements>
                                    <element id="SV201">0636</element>
                                    <element id="SV202">HC:J7030:::::PHARMACY</element>
                                    <element id="SV203">350</element>
                                    <element id="SV204">UN</element>
                                    <element id="SV205">1</element>
                                  </elements>
                                </segment>
                                <segment id="DTP">
                                  <elements>
                                    <element id="DTP01">472</element>
                                    <element id="DTP02">D8</element>
                                    <element id="DTP03">20200124</element>
                                  </elements>
                                </segment>
                                <segment id="REF">
                                  <elements>
                                    <element id="REF01">6R</element>
                                    <element id="REF02">A458-1064441-00004</element>
                                  </elements>
                                </segment>
                              </segments>
                              <loops>
                                <loop id="2410">
                                  <segments>
                                    <segment id="LIN">
                                      <elements>
                                        <element id="LIN01"></element>
                                        <element id="LIN02">N4</element>
                                        <element id="LIN03">00338004904</element>
                                      </elements>
                                    </segment>
                                    <segment id="CTP">
                                      <elements>
                                        <element id="CTP01"></element>
                                        <element id="CTP02"></element>
                                        <element id="CTP03"></element>
                                        <element id="CTP04">1000</element>
                                        <element id="CTP05">ML</element>
                                      </elements>
                                    </segment>
                                  </segments>
                                  <loops/>
                                </loop>
                                <loop id="2430">
                                  <segments>
                                    <segment id="SVD">
                                      <elements>
                                        <element id="SVD01">03101</element>
                                        <element id="SVD02">0</element>
                                        <element id="SVD03">HC:J7030</element>
                                        <element id="SVD04">0636</element>
                                        <element id="SVD05">1</element>
                                      </elements>
                                    </segment>
                                    <segment id="CAS">
                                      <elements>
                                        <element id="CAS01">CO</element>
                                        <element id="CAS02">97</element>
                                        <element id="CAS03">350</element>
                                        <element id="CAS04">1</element>
                                      </elements>
                                    </segment>
                                    <segment id="DTP">
                                      <elements>
                                        <element id="DTP01">573</element>
                                        <element id="DTP02">D8</element>
                                        <element id="DTP03">20200630</element>
                                      </elements>
                                    </segment>
                                  </segments>
                                  <loops/>
                                </loop>
                              </loops>
                            </loop>
                          </loops>
                        </loop>
                      </loops>
                    </loop>
                  </loops>
                </loop>
              </loops>
            </loop>
          </loops>
        </loop>
      </loops>
    </loop>
  </loops>
</loop>