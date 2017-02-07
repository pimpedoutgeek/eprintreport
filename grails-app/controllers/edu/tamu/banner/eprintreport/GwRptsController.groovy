package edu.tamu.banner.eprintreport

import edu.tamu.compassreport.CsvXls
import edu.tamu.compassreport.ToHtml
import edu.tamu.compassreport.WriteBlob
import grails.converters.JSON
import org.apache.commons.io.IOUtils
import org.apache.commons.io.monitor.FileEntry
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class GwRptsController {

    def compassReportsService

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
//        response.contentType = "application/json"
        params.max = Math.min(max ?: 10, 100)
        respond GwRpts.list(params), model:[gwRptsInstanceCount: GwRpts.count()]
    }

    def show(GwRpts gwRptsInstance) {
        BigDecimal bigint = new BigDecimal(21)

        byte[] b = compassReportsService.getGwRptsBlobPDFBytes(bigint)

        response.setHeader("Expires","0")
        response.setHeader("Content-disposition", "attachment; filename=file.pdf")
        response.setContentType("application/pdf")
        response.setContentLength(b.length)
        response.getOutputStream().write(b)
        response.getOutputStream().flush()

//        respond gwRptsInstance
    }

    def create() {
        respond new GwRpts(params)
    }

    def gwrptsToJSON(final String report) {
        response.contentType = "application/json"
        def gwrpts = GwRpts.findAllByGwRptsObjectName(report)
        def result = [gwRptsInstance: gwrpts]
//        render "Params: report = $report"
        render result as JSON
//        def result = compassReportsService.getCompassReportsAsJSON('GURPDED')
    }

    def gwrptsBlobToJSON(int seq) { //http://localhost:8080/EprintReport/gwrptsBlobToJSON?seq=21

        BigDecimal bigint = new BigDecimal(seq)

//        byte[] b = compassReportsService.getGwRptsBlobPDFBytes(bigint)

        byte[] b = compassReportsService.readBlobToByte(bigint)
        response.setHeader("Expires","0")
        response.setHeader("Content-disposition", "attachment; filename=file.pdf")
        response.setContentType("application/pdf")
        response.setContentLength(b.length)
        response.getOutputStream().write(b, 0, b.length)
        response.getOutputStream()
        response.getOutputStream().flush()
        response.setStatus(200)
//        response.getOutputStream().close()
        /*def encoded = b.encodeHex()
        encoded*/


    }

    def gwrptsSeqNameBlob(final String name) { //http://localhost:8080/EprintReport/gwrptsSNB?name=tgrfeed
        log.debug "gwrptsSeqNameBlob: ${name}"

        def gwrpts = compassReportsService.getCompassReportsAsJSON(name)
        render gwrpts
    }

    def gwrptsBlob(final BigDecimal seq) {
        def gwrpts = compassReportsService.getGwRptsBlobAsJSON(seq)
        render gwrpts
    }

    byte[] gwrptsBlobAsByte(final BigDecimal seq) {
        byte[] gwrptsbytes = compassReportsService.getGwRptsBlobPDFBytes(seq)
        gwrptsbytes
    }

    byte[] gwrptsBlobBytes(final BigDecimal seq) {
        byte[] gwrptsblobbytes = compassReportsService.getGwRptsBlobBytes(seq);
        gwrptsblobbytes
    }

    byte[] getReadBlob(final BigDecimal seq) {
        byte[] blob = compassReportsService.readBlobToByte(seq)
        blob
    }

    def writeBlob(String filename) {

        def tok = filename.tokenize(".")
        def name = tok[0]
        def ext = tok[1]
        BigDecimal bigDec = new BigDecimal(name)

        WriteBlob writeBlob = new WriteBlob()

        def total = compassReportsService.writeBlobToFile(bigDec, writeBlob)

        render filename

    }

    def upload(String fileName) { //gwrptsFileUpload @controller

        log.debug "upload: ${fileName}"

        def input = null
        def paths = servletContext.getResourcePaths("/WEB-INF/files")
        paths.each {
            log.debug "path: ${it}"
            println "path ${it}"
        }

        def realpath = servletContext.getRealPath("/WEB-INF/files")

        def (name, mime) = fileName.tokenize('.')

        switch (mime.toLowerCase()) {
            case 'pdf' :
                        input = servletContext.getResourceAsStream("/WEB-INF/files/" + fileName).bytes.encodeAsBase64()
                break
            case 'lis' : input = servletContext.getResourceAsStream("/WEB-INF/files/" + fileName).getText('UTF-8')
                break
            case 'txt' : input = servletContext.getResourceAsStream("/WEB-INF/files/" + fileName).getText('UTF-8')
                break
            case 'log' : input = servletContext.getResourceAsStream("/WEB-INF/files/" + fileName).getText('UTF-8')
                break
            case 'csv' :
                        input = servletContext.getResourceAsStream("/WEB-INF/files/" + fileName).getText('UTF-8')
                        CsvXls.csvToXLS(input, name, realpath)
                        ToHtml toHtml = ToHtml.create(realpath+"/"+name+".xls", new PrintWriter(new FileWriter(realpath+"/"+name+".html")))
                        toHtml.setCompleteHTML(true)
                        toHtml.printPage()
                        input = servletContext.getResourceAsStream("/WEB-INF/files/" + name+".html").getText('UTF-8')
                break
        }

        render input

    }

    def saveUpload(){
        String fileLabel = params.fileLabel
        MultipartFile uploadedFile = null

        String fileName=""
        try{
            if (request instanceof MultipartHttpServletRequest){
                //Get the file's name from request
                fileName = request.getFileNames()[0]
                //Get a reference to the uploaded file.
                uploadedFile = request.getFile(fileName)

                println "File name : ${uploadedFile.originalFilename}"
                println "File size : ${uploadedFile.size}"
                println "File label :"+ ${fileLabel}
                //appending file extension. In my requirement, user can modify original file name providing fileLabel. So, if user provides fileLabel, we //should save file with that name.
                fileLabel +=".${uploadedFile.originalFilename.split("\\.")[-1]}"

            }
            if (uploadedFile.empty) {
                flash.error = g.message(code: uploadedFile.size, default:'Empty cannot be uploaded') // put in uploadedFile.size instead of blank: ''
                redirect(action: 'show',id: user.id)
                return
            }
            //get uploaded file's inputStream
            InputStream inputStream = uploadedFile.inputStream
            //get the file storage location
//            def fileTobeStoredInDirPath = grailsApplication.config.myapp.file.storage.location
            def fileTobeStoredInDirPath = grailsApplication.config.EprintReport.file.storage.location
            //create a new file with fileLabel
            File file = new File(fileTobeStoredInDirPath, fileLabel)
            //This support both overriding and creating new file
            //If two of these fails, that means got some internal issue. May be new file creation permissions issue
            if (file.exists() || file.createNewFile()) {

                //to close the fileOutStream, opening it using withOutStream closure
                file.withOutputStream{fos->
                    IOUtils.copyLarge(inputStream, fos)
                }
                FileEntry fileEntry = new FileEntry()
                fileEntry.createdUser = session.user.id
                fileEntry.fileLabel = fileLabel
                fileEntry.save(flush:true)
                flash.success = g.message(code: null, default:'File uploaded successfully')
                redirect(action: 'show',id: user.id)
            }else{
                throw new RuntimeException("error while creating  ${file} at ${fileTobeStoredInDirPath}")
            }
        }
        catch (Exception e){
            flash.error = g.message(code: null, default: 'File upload failed due to internal errors. Please try again')
//            redirect(action: 'show',id: user.id)
            redirect(action: 'notFound')
        }
    }

    @Transactional
    def save(GwRpts gwRptsInstance) {
        if (gwRptsInstance == null) {
            notFound()
            return
        }

        if (gwRptsInstance.hasErrors()) {
            respond gwRptsInstance.errors, view:'create'
            return
        }

        gwRptsInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'gwRpts.label', default: 'GwRpts'), gwRptsInstance.id])
                redirect gwRptsInstance
            }
            '*' { respond gwRptsInstance, [status: CREATED] }
        }
    }

    def edit(GwRpts gwRptsInstance) {
        respond gwRptsInstance
    }

    @Transactional
    def update(GwRpts gwRptsInstance) {
        if (gwRptsInstance == null) {
            notFound()
            return
        }

        if (gwRptsInstance.hasErrors()) {
            respond gwRptsInstance.errors, view:'edit'
            return
        }

        gwRptsInstance.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'GwRpts.label', default: 'GwRpts'), gwRptsInstance.id])
                redirect gwRptsInstance
            }
            '*'{ respond gwRptsInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(GwRpts gwRptsInstance) {

        if (gwRptsInstance == null) {
            notFound()
            return
        }

        gwRptsInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'GwRpts.label', default: 'GwRpts'), gwRptsInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'gwRpts.label', default: 'GwRpts'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
