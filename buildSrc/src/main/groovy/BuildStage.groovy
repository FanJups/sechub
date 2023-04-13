/**
 * Special build stage class. Because we need compiled java code to generate our open api file,
 * the java api generation - which needs the open api file + a java compile - cannot happen 
 * on same "stage".
 * To provide this, we have introduced the term sechub build stage - when stage "api-necessary" is
 * used (or no stage is set), the parts which need a generated open api file will be included
 * as well. 
 */
class BuildStage{
    
    private static final String STAGE_ALL = "all"; 
    private static final String STAGE_WITHOUT_API = "without-api"; 
    private static final String STAGE_API_NECESSARY = "api-necessary"; 
    
    private String stage;
    private boolean openApiFileMustExist;
    private boolean acceptAll;
    
    BuildStage(){
        stage = System.getProperty("sechub.build.stage");
        if(stage==null|| stage.isEmpty()){
            stage = STAGE_ALL;
        }

        switch(stage){
            case STAGE_ALL:
                // we just do not define any constraints here
                // means: all is importable by IDEs
                acceptAll=true;
                break;
             case STAGE_WITHOUT_API:
                openApiFileMustExist=false;
                break;
             case STAGE_API_NECESSARY:
                openApiFileMustExist=true;
                break;
            default: 
                throw new IllegalArgumentException("Unknown build stage:"+ stage);
        }
          
    }
    
    public boolean providesGeneratedOpenApiFile(){
        return acceptAll || openApiFileMustExist;                  
    }

}
