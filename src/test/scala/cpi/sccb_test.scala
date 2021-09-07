package sislab.cpi_test

import chisel3._
import chisel3.util._
import chisel3.iotesters._
import chisel3.iotesters.Driver
import org.scalatest._

import scala.util.control.Breaks

import sislab.cpi.SCCB_interface
class SCCB_interface_test(dut:SCCB_interface)(n_of_random_test: Int) extends PeekPokeTester(dut: SCCB_interface){

  poke(dut.io.config,false)
  step(1000)

  val number_of_test=(n_of_random_test)
  var number_of_tests_passed=0
  for ( n_test<- 0 until number_of_test){
    val slave_addr=0x42
    val control_addr=scala.util.Random.nextInt(255)
    val config_data=scala.util.Random.nextInt(255)
    poke(dut.io.control_address, control_addr)
    poke(dut.io.config_data, config_data)
    poke(dut.io.config,false.B)
    step(scala.util.Random.nextInt(10))
    poke(dut.io.config,true.B)
    step(1)
    poke(dut.io.config,false.B)
    step(1)
    var data_bit_idx=7
    val transmitted_slave_addr=Array.fill(8){0}
    val transmitted_addr=Array.fill(8){0}
    val transmitted_data=Array.fill(8){0}
    var phase=0
    val loop=new Breaks
    loop.breakable{
      while(peek(dut.io.sccb_ready)==0){
        var c_low=peek(dut.io.SIOC)
        step(1)
        var c_high=peek(dut.io.SIOC)
        if(c_high-c_low==1){
          if(phase==0&&(data_bit_idx!=(-1))){
            transmitted_slave_addr(data_bit_idx)=peek(dut.io.SIOD).toInt
          }
          else if(phase==1&&(data_bit_idx!=(-1))){
            transmitted_addr(data_bit_idx)=peek(dut.io.SIOD).toInt
          }
          else if(phase==2&&(data_bit_idx!=(-1))){
            transmitted_data(data_bit_idx)=peek(dut.io.SIOD).toInt
          }
          data_bit_idx=data_bit_idx-1

          if(data_bit_idx==(-2)){
            data_bit_idx=7
            phase=phase+1
          }
        }
      }
      if(peek(dut.io.sccb_ready)==1){
        step(5)
        loop.break
      }
    }
    step(200)

    if(slave_addr==bin2dec(transmitted_slave_addr)){
      if((control_addr==bin2dec(transmitted_addr))&&(config_data==bin2dec(transmitted_data))){
        number_of_tests_passed=number_of_tests_passed+1
      }
    }
  }
  Console.out.println(Console.BLUE+"blue texts indicate the testing results")
  Console.out.println(Console.BLUE+"test result: " +number_of_tests_passed.toString+
    " tests passed over "+number_of_test.toString+" being tested"+Console.RESET)
  def bin2dec(in:Array[Int]): Int={
    val array_length=in.length
    var dec=0
    for(idx<-0 until array_length){
      dec=dec+in(idx)*scala.math.pow(2,idx).toInt
    }
    dec
  }

}

class SCCB_interface_waveform extends FlatSpec with Matchers {
  "WaveformCounter" should "pass" in {
    Driver.execute(Array("--generate-vcd-output", "on"), () =>
      new SCCB_interface(50, 100.2.toFloat )) { c =>
      new SCCB_interface_test(c)(20)
    } should be (true)
  }
}
object SCCB_interface_test extends App{
  chisel3.iotesters.Driver(() => new SCCB_interface(50, 100.2.toFloat)){ c=>
    new SCCB_interface_test(c)(728)
  }
}