package com.pb.despair.pi.tasks;




/**
 * A sample PI task which sends out dummy iteration statistic messages.
 *
 * @author Tim Heier
 * @version 1.0, 12/27/2002
 */

public class PretendPI {


//    protected Logger logger = Logger.getLogger("com.pb.despair.pi");
//    String[] commodities = {"cereal grains","alcoholic beverages","tobacco products",
//                            "log/wood in the rough","wood",
//                            "newsprint paper/paperboard","pulp/paperboard articles","printed",
//                            "textiles/leather","furn./lighting/illum. signs","misc. manufactured"};
//
//    //Simulate PI doing some work and generating iteration statistics.
//    public void doWork() {
//
//        int iterations=0;
//        double newMeritMeasure= 0;
//        double stepScaling = 0.5;
//
//        DashboardDataBean meritDataBean = createMeritMeasureDataBean(iterations,newMeritMeasure);
//        DashboardDataBean stepScaleDataBean = createStepScaleDataBean(iterations, stepScaling);
//
//        Message PIDashboardMessage = createPIDashboardMessage(meritDataBean,"PIMeritMeasure");
//        publishStatusMessage(PIDashboardMessage);
//
//        PIDashboardMessage = createPIDashboardMessage(stepScaleDataBean,"PIStepScale");
//        publishStatusMessage(PIDashboardMessage);
//
//        for(int i=0;i<commodities.length;i++){
//
//            iterations++;
//            //Simulate going thru each commodity and deciding new surplus values,
//            //prices, buy and sell quantities
//            //and import export quantities.
//                    try {
//                        Thread.sleep( 5000 );
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//
//            newMeritMeasure= 50*(Math.pow((10.0/11.0),(double)i));
//            stepScaling = (Math.random()*.2)-.1;  //random number between -.1 and .1
//
//        meritDataBean = createMeritMeasureDataBean(iterations,newMeritMeasure);
//        stepScaleDataBean = createStepScaleDataBean(iterations, stepScaling);
//
//        PIDashboardMessage = createPIDashboardMessage(meritDataBean,DashboardDataKey.MERIT_MEASURE_CACHE_NAME);
//        publishStatusMessage(PIDashboardMessage);
//
//        PIDashboardMessage = createPIDashboardMessage(stepScaleDataBean,DashboardDataKey.STEP_SCALE_CACHE_NAME);
//        publishStatusMessage(PIDashboardMessage);
//
//        logger.info("iteration number " + i + " completed");
//
//        }
//
//    }
//
//
//    /** Helper method for task.
//     */
//    private Message createPIDashboardMessage(DashboardDataBean dataBean, String messageID) {
//        Message PIDashboardMessage = MessageFactory.getInstance().createMessage();
//
//        PIDashboardMessage.setId(messageID);
//        PIDashboardMessage.setValue(DashboardDataKey.DASHBOARDDATA_BEAN, dataBean);
//
//        return PIDashboardMessage;
//    }
//
//    private DashboardDataBean createMeritMeasureDataBean (int iterationNum, double meritMeasure){
//        DashboardDataBean dataBean = new DashboardDataBean();
//        dataBean.setValue(DashboardDataKey.ITERATION_NUMBER,(double)iterationNum);
//        dataBean.setValue(DashboardDataKey.MERIT_MEASURE, meritMeasure);
//        return dataBean;
//    }
//
//    private DashboardDataBean createStepScaleDataBean (int iterationNum, double stepScale){
//        DashboardDataBean dataBean = new DashboardDataBean();
//        dataBean.setValue(DashboardDataKey.ITERATION_NUMBER, (double)iterationNum);
//        dataBean.setValue(DashboardDataKey.STEP_SCALE, stepScale);
//        return dataBean;
//    }
}
