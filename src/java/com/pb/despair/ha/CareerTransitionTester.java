package com.pb.tlumip.ha;

/**
 * @author jabraham
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class CareerTransitionTester {

	public static void main(String[] args) {
		int[][] statusCounter = new int[80][4];
	    double[] cumulativeSchoolCounter = new double[80];
	    double[] cumulativeWorkCounter = new double[80];
	    final int numSims =10000;
		for (int i=0;i<numSims;i++) {
			int school = 10;
			int experience = 0;
			int oldStatus = 3;
			for (int age=16;age<80;age++) {
				double u1 = 0;
				double u2 = 0;
				if (age>=18 && age <=25) u2+=1.461 ;
				if (age>=26 && age <=65) u2+=3.967 ;
				if (age>=66)             u2+=0.065 ;
				if (age<5)               u2+=1.338 ;
				if (age>=6&&age<=15)     u2+=-0.015;
				if (school<12)           u2+=2.753 ;
				if (school==12)          u2+=1.857 ;
				if (school>12&&school<16)u2+=0.734 ;
				if (school==16)          u2+=1.494 ;
				                         u2+=-1.790 ;
//				                         u2+=1.338 ;
				if(oldStatus==1)         u2+=-0.861;
				if(oldStatus==2)         u2+=2.000 ;
				if(oldStatus==3)         u2+=-2.689;
				double u3 = 0;                     ;
				if (age>=18 && age <=25) u3+=-0.102;
				if (age>=26 && age <=65) u3+=-1.679;
				if (age>=66)             u3+=0     ;
				if (age<5)               u3+=0.333 ;
				if (age>=6&&age<=15)     u3+=0.198 ;
				if (school<12)           u3+=0.964 ;
				if (school==12)          u3+=-0.400;
				if (school>12&&school<16)u3+=-0.019;
				if (school==16)          u3+=-0.726;
				                         u3+=0.495 ;
//				                         u3+=0.333 ;
				if(oldStatus==1)         u3+=-0.599;
				if(oldStatus==2)         u3+=-1.029;
				if(oldStatus==3)         u3+=0.502 ;
				double u4 = 0;                     ;
                if (age<=17)             u4+=-3.106;
				if (age>=18 && age <=25) u4+=-0.798;
				if (age>=26 && age <=65) u4+=1.746 ;
				if (age>=66)             u4+=0     ;
				if (age<5)               u4+=1.476 ;
				if (age>=6&&age<=15)     u4+=-0.226;
				if (school<12)           u4+=5.976 ;
				if (school==12)          u4+=4.735 ;
				if (school>12&&school<16)u4+=3.277 ;
				if (school==16)          u4+=3.483 ;
				                         u4+=-1.516 ;
//				                         u4+=1.476 ;
				if(oldStatus==1)         u4+=-4.125;
				if(oldStatus==2)         u4+=-1.210;
				if(oldStatus==3)         u4+=-3.073;
				
				double expu1 = Math.exp(u1);
				double expu2 = Math.exp(u2);
				double expu3 = Math.exp(u3);
				double expu4 = Math.exp(u4);
				double denom = expu1+expu2+expu3+expu4;
				double selector = denom*Math.random();
				if (selector<expu1) oldStatus = 1;
				else if (selector<expu2+expu1) oldStatus = 2;
				else if (selector<expu1+expu2+expu3) oldStatus = 3;
				else oldStatus = 4;
				cumulativeSchoolCounter[age]+=school;
				cumulativeWorkCounter[age]+=experience;
				if (oldStatus ==1 || oldStatus==3) school++;
				if (oldStatus==1 || oldStatus ==2) experience++;
				statusCounter[age][oldStatus-1]++;
			}
				
		}
		System.out.println("simulating "+numSims);
		System.out.println("age\tWS\tWNS\tNWS\tNWNS\tE(sc)\tE(exprnce)");
		for (int age=0;age<80;age++) {
			System.out.println(age+"\t"+statusCounter[age][0]+"\t"+statusCounter[age][1]+"\t"+statusCounter[age][2]+"\t"+statusCounter[age][3]+"\t"+cumulativeSchoolCounter[age]/numSims+"\t"+cumulativeWorkCounter[age]/numSims);
		}
		
	}
}
