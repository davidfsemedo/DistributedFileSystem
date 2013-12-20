
package trab1.both.ws;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the trab1.both.ws package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _IOException_QNAME = new QName("http://tp2/", "IOException");
    private final static QName _GetServerListResponse_QNAME = new QName("http://tp2/", "getServerListResponse");
    private final static QName _GetServerList_QNAME = new QName("http://tp2/", "getServerList");
    private final static QName _CreateDir_QNAME = new QName("http://tp2/", "createDir");
    private final static QName _UpdateKnownServerList_QNAME = new QName("http://tp2/", "updateKnownServerList");
    private final static QName _ReceiveShutdownAnnouncementResponse_QNAME = new QName("http://tp2/", "receiveShutdownAnnouncementResponse");
    private final static QName _Dir_QNAME = new QName("http://tp2/", "dir");
    private final static QName _GetNameResponse_QNAME = new QName("http://tp2/", "getNameResponse");
    private final static QName _NewServerConnected_QNAME = new QName("http://tp2/", "newServerConnected");
    private final static QName _CopyFileServerToServer_QNAME = new QName("http://tp2/", "copyFileServerToServer");
    private final static QName _DirResponse_QNAME = new QName("http://tp2/", "dirResponse");
    private final static QName _GetCommunity_QNAME = new QName("http://tp2/", "getCommunity");
    private final static QName _RemoveFileResponse_QNAME = new QName("http://tp2/", "removeFileResponse");
    private final static QName _CreateDirResponse_QNAME = new QName("http://tp2/", "createDirResponse");
    private final static QName _ReceiveShutdownAnnouncement_QNAME = new QName("http://tp2/", "receiveShutdownAnnouncement");
    private final static QName _GetUrlResponse_QNAME = new QName("http://tp2/", "getUrlResponse");
    private final static QName _RemoveDirectoryResponse_QNAME = new QName("http://tp2/", "removeDirectoryResponse");
    private final static QName _PushFileResponse_QNAME = new QName("http://tp2/", "pushFileResponse");
    private final static QName _NewServerConnectedResponse_QNAME = new QName("http://tp2/", "newServerConnectedResponse");
    private final static QName _GetFileInfo_QNAME = new QName("http://tp2/", "getFileInfo");
    private final static QName _PushFile_QNAME = new QName("http://tp2/", "pushFile");
    private final static QName _CopyFileServerToServerResponse_QNAME = new QName("http://tp2/", "copyFileServerToServerResponse");
    private final static QName _InfoNotFoundException_QNAME = new QName("http://tp2/", "InfoNotFoundException");
    private final static QName _RemoveFile_QNAME = new QName("http://tp2/", "removeFile");
    private final static QName _RemoveDirectory_QNAME = new QName("http://tp2/", "removeDirectory");
    private final static QName _PullFile_QNAME = new QName("http://tp2/", "pullFile");
    private final static QName _GetName_QNAME = new QName("http://tp2/", "getName");
    private final static QName _GetCommunityResponse_QNAME = new QName("http://tp2/", "getCommunityResponse");
    private final static QName _GetUrl_QNAME = new QName("http://tp2/", "getUrl");
    private final static QName _GetNextIdResponse_QNAME = new QName("http://tp2/", "getNextIdResponse");
    private final static QName _GetFileInfoResponse_QNAME = new QName("http://tp2/", "getFileInfoResponse");
    private final static QName _UpdateKnownServerListResponse_QNAME = new QName("http://tp2/", "updateKnownServerListResponse");
    private final static QName _GetNextId_QNAME = new QName("http://tp2/", "getNextId");
    private final static QName _PullFileResponse_QNAME = new QName("http://tp2/", "pullFileResponse");
    private final static QName _PushFileArg2_QNAME = new QName("", "arg2");
    private final static QName _PullFileResponseReturn_QNAME = new QName("", "return");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: trab1.both.ws
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link NewServerConnectedResponse }
     * 
     */
    public NewServerConnectedResponse createNewServerConnectedResponse() {
        return new NewServerConnectedResponse();
    }

    /**
     * Create an instance of {@link PushFileResponse }
     * 
     */
    public PushFileResponse createPushFileResponse() {
        return new PushFileResponse();
    }

    /**
     * Create an instance of {@link ReceiveShutdownAnnouncement }
     * 
     */
    public ReceiveShutdownAnnouncement createReceiveShutdownAnnouncement() {
        return new ReceiveShutdownAnnouncement();
    }

    /**
     * Create an instance of {@link CreateDirResponse }
     * 
     */
    public CreateDirResponse createCreateDirResponse() {
        return new CreateDirResponse();
    }

    /**
     * Create an instance of {@link NewServerConnected }
     * 
     */
    public NewServerConnected createNewServerConnected() {
        return new NewServerConnected();
    }

    /**
     * Create an instance of {@link GetFileInfoResponse }
     * 
     */
    public GetFileInfoResponse createGetFileInfoResponse() {
        return new GetFileInfoResponse();
    }

    /**
     * Create an instance of {@link UpdateKnownServerListResponse }
     * 
     */
    public UpdateKnownServerListResponse createUpdateKnownServerListResponse() {
        return new UpdateKnownServerListResponse();
    }

    /**
     * Create an instance of {@link GetNextId }
     * 
     */
    public GetNextId createGetNextId() {
        return new GetNextId();
    }

    /**
     * Create an instance of {@link InfoNotFoundException }
     * 
     */
    public InfoNotFoundException createInfoNotFoundException() {
        return new InfoNotFoundException();
    }

    /**
     * Create an instance of {@link UpdateKnownServerList }
     * 
     */
    public UpdateKnownServerList createUpdateKnownServerList() {
        return new UpdateKnownServerList();
    }

    /**
     * Create an instance of {@link GetUrlResponse }
     * 
     */
    public GetUrlResponse createGetUrlResponse() {
        return new GetUrlResponse();
    }

    /**
     * Create an instance of {@link RemoveDirectoryResponse }
     * 
     */
    public RemoveDirectoryResponse createRemoveDirectoryResponse() {
        return new RemoveDirectoryResponse();
    }

    /**
     * Create an instance of {@link GetCommunity }
     * 
     */
    public GetCommunity createGetCommunity() {
        return new GetCommunity();
    }

    /**
     * Create an instance of {@link GetNameResponse }
     * 
     */
    public GetNameResponse createGetNameResponse() {
        return new GetNameResponse();
    }

    /**
     * Create an instance of {@link CreateDir }
     * 
     */
    public CreateDir createCreateDir() {
        return new CreateDir();
    }

    /**
     * Create an instance of {@link PullFile }
     * 
     */
    public PullFile createPullFile() {
        return new PullFile();
    }

    /**
     * Create an instance of {@link GetName }
     * 
     */
    public GetName createGetName() {
        return new GetName();
    }

    /**
     * Create an instance of {@link GetServerListResponse }
     * 
     */
    public GetServerListResponse createGetServerListResponse() {
        return new GetServerListResponse();
    }

    /**
     * Create an instance of {@link RemoveFileResponse }
     * 
     */
    public RemoveFileResponse createRemoveFileResponse() {
        return new RemoveFileResponse();
    }

    /**
     * Create an instance of {@link GetServerList }
     * 
     */
    public GetServerList createGetServerList() {
        return new GetServerList();
    }

    /**
     * Create an instance of {@link GetCommunityResponse }
     * 
     */
    public GetCommunityResponse createGetCommunityResponse() {
        return new GetCommunityResponse();
    }

    /**
     * Create an instance of {@link ReceiveShutdownAnnouncementResponse }
     * 
     */
    public ReceiveShutdownAnnouncementResponse createReceiveShutdownAnnouncementResponse() {
        return new ReceiveShutdownAnnouncementResponse();
    }

    /**
     * Create an instance of {@link RemoveFile }
     * 
     */
    public RemoveFile createRemoveFile() {
        return new RemoveFile();
    }

    /**
     * Create an instance of {@link IOException }
     * 
     */
    public IOException createIOException() {
        return new IOException();
    }

    /**
     * Create an instance of {@link FileInfo }
     * 
     */
    public FileInfo createFileInfo() {
        return new FileInfo();
    }

    /**
     * Create an instance of {@link Dir }
     * 
     */
    public Dir createDir() {
        return new Dir();
    }

    /**
     * Create an instance of {@link CopyFileServerToServerResponse }
     * 
     */
    public CopyFileServerToServerResponse createCopyFileServerToServerResponse() {
        return new CopyFileServerToServerResponse();
    }

    /**
     * Create an instance of {@link GetFileInfo }
     * 
     */
    public GetFileInfo createGetFileInfo() {
        return new GetFileInfo();
    }

    /**
     * Create an instance of {@link RemoveDirectory }
     * 
     */
    public RemoveDirectory createRemoveDirectory() {
        return new RemoveDirectory();
    }

    /**
     * Create an instance of {@link DirResponse }
     * 
     */
    public DirResponse createDirResponse() {
        return new DirResponse();
    }

    /**
     * Create an instance of {@link GetNextIdResponse }
     * 
     */
    public GetNextIdResponse createGetNextIdResponse() {
        return new GetNextIdResponse();
    }

    /**
     * Create an instance of {@link PushFile }
     * 
     */
    public PushFile createPushFile() {
        return new PushFile();
    }

    /**
     * Create an instance of {@link CopyFileServerToServer }
     * 
     */
    public CopyFileServerToServer createCopyFileServerToServer() {
        return new CopyFileServerToServer();
    }

    /**
     * Create an instance of {@link GetUrl }
     * 
     */
    public GetUrl createGetUrl() {
        return new GetUrl();
    }

    /**
     * Create an instance of {@link PullFileResponse }
     * 
     */
    public PullFileResponse createPullFileResponse() {
        return new PullFileResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IOException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "IOException")
    public JAXBElement<IOException> createIOException(IOException value) {
        return new JAXBElement<IOException>(_IOException_QNAME, IOException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetServerListResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "getServerListResponse")
    public JAXBElement<GetServerListResponse> createGetServerListResponse(GetServerListResponse value) {
        return new JAXBElement<GetServerListResponse>(_GetServerListResponse_QNAME, GetServerListResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetServerList }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "getServerList")
    public JAXBElement<GetServerList> createGetServerList(GetServerList value) {
        return new JAXBElement<GetServerList>(_GetServerList_QNAME, GetServerList.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CreateDir }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "createDir")
    public JAXBElement<CreateDir> createCreateDir(CreateDir value) {
        return new JAXBElement<CreateDir>(_CreateDir_QNAME, CreateDir.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UpdateKnownServerList }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "updateKnownServerList")
    public JAXBElement<UpdateKnownServerList> createUpdateKnownServerList(UpdateKnownServerList value) {
        return new JAXBElement<UpdateKnownServerList>(_UpdateKnownServerList_QNAME, UpdateKnownServerList.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReceiveShutdownAnnouncementResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "receiveShutdownAnnouncementResponse")
    public JAXBElement<ReceiveShutdownAnnouncementResponse> createReceiveShutdownAnnouncementResponse(ReceiveShutdownAnnouncementResponse value) {
        return new JAXBElement<ReceiveShutdownAnnouncementResponse>(_ReceiveShutdownAnnouncementResponse_QNAME, ReceiveShutdownAnnouncementResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Dir }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "dir")
    public JAXBElement<Dir> createDir(Dir value) {
        return new JAXBElement<Dir>(_Dir_QNAME, Dir.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetNameResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "getNameResponse")
    public JAXBElement<GetNameResponse> createGetNameResponse(GetNameResponse value) {
        return new JAXBElement<GetNameResponse>(_GetNameResponse_QNAME, GetNameResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NewServerConnected }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "newServerConnected")
    public JAXBElement<NewServerConnected> createNewServerConnected(NewServerConnected value) {
        return new JAXBElement<NewServerConnected>(_NewServerConnected_QNAME, NewServerConnected.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CopyFileServerToServer }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "copyFileServerToServer")
    public JAXBElement<CopyFileServerToServer> createCopyFileServerToServer(CopyFileServerToServer value) {
        return new JAXBElement<CopyFileServerToServer>(_CopyFileServerToServer_QNAME, CopyFileServerToServer.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DirResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "dirResponse")
    public JAXBElement<DirResponse> createDirResponse(DirResponse value) {
        return new JAXBElement<DirResponse>(_DirResponse_QNAME, DirResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetCommunity }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "getCommunity")
    public JAXBElement<GetCommunity> createGetCommunity(GetCommunity value) {
        return new JAXBElement<GetCommunity>(_GetCommunity_QNAME, GetCommunity.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RemoveFileResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "removeFileResponse")
    public JAXBElement<RemoveFileResponse> createRemoveFileResponse(RemoveFileResponse value) {
        return new JAXBElement<RemoveFileResponse>(_RemoveFileResponse_QNAME, RemoveFileResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CreateDirResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "createDirResponse")
    public JAXBElement<CreateDirResponse> createCreateDirResponse(CreateDirResponse value) {
        return new JAXBElement<CreateDirResponse>(_CreateDirResponse_QNAME, CreateDirResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReceiveShutdownAnnouncement }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "receiveShutdownAnnouncement")
    public JAXBElement<ReceiveShutdownAnnouncement> createReceiveShutdownAnnouncement(ReceiveShutdownAnnouncement value) {
        return new JAXBElement<ReceiveShutdownAnnouncement>(_ReceiveShutdownAnnouncement_QNAME, ReceiveShutdownAnnouncement.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetUrlResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "getUrlResponse")
    public JAXBElement<GetUrlResponse> createGetUrlResponse(GetUrlResponse value) {
        return new JAXBElement<GetUrlResponse>(_GetUrlResponse_QNAME, GetUrlResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RemoveDirectoryResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "removeDirectoryResponse")
    public JAXBElement<RemoveDirectoryResponse> createRemoveDirectoryResponse(RemoveDirectoryResponse value) {
        return new JAXBElement<RemoveDirectoryResponse>(_RemoveDirectoryResponse_QNAME, RemoveDirectoryResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PushFileResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "pushFileResponse")
    public JAXBElement<PushFileResponse> createPushFileResponse(PushFileResponse value) {
        return new JAXBElement<PushFileResponse>(_PushFileResponse_QNAME, PushFileResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NewServerConnectedResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "newServerConnectedResponse")
    public JAXBElement<NewServerConnectedResponse> createNewServerConnectedResponse(NewServerConnectedResponse value) {
        return new JAXBElement<NewServerConnectedResponse>(_NewServerConnectedResponse_QNAME, NewServerConnectedResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetFileInfo }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "getFileInfo")
    public JAXBElement<GetFileInfo> createGetFileInfo(GetFileInfo value) {
        return new JAXBElement<GetFileInfo>(_GetFileInfo_QNAME, GetFileInfo.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PushFile }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "pushFile")
    public JAXBElement<PushFile> createPushFile(PushFile value) {
        return new JAXBElement<PushFile>(_PushFile_QNAME, PushFile.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CopyFileServerToServerResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "copyFileServerToServerResponse")
    public JAXBElement<CopyFileServerToServerResponse> createCopyFileServerToServerResponse(CopyFileServerToServerResponse value) {
        return new JAXBElement<CopyFileServerToServerResponse>(_CopyFileServerToServerResponse_QNAME, CopyFileServerToServerResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InfoNotFoundException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "InfoNotFoundException")
    public JAXBElement<InfoNotFoundException> createInfoNotFoundException(InfoNotFoundException value) {
        return new JAXBElement<InfoNotFoundException>(_InfoNotFoundException_QNAME, InfoNotFoundException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RemoveFile }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "removeFile")
    public JAXBElement<RemoveFile> createRemoveFile(RemoveFile value) {
        return new JAXBElement<RemoveFile>(_RemoveFile_QNAME, RemoveFile.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RemoveDirectory }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "removeDirectory")
    public JAXBElement<RemoveDirectory> createRemoveDirectory(RemoveDirectory value) {
        return new JAXBElement<RemoveDirectory>(_RemoveDirectory_QNAME, RemoveDirectory.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PullFile }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "pullFile")
    public JAXBElement<PullFile> createPullFile(PullFile value) {
        return new JAXBElement<PullFile>(_PullFile_QNAME, PullFile.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetName }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "getName")
    public JAXBElement<GetName> createGetName(GetName value) {
        return new JAXBElement<GetName>(_GetName_QNAME, GetName.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetCommunityResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "getCommunityResponse")
    public JAXBElement<GetCommunityResponse> createGetCommunityResponse(GetCommunityResponse value) {
        return new JAXBElement<GetCommunityResponse>(_GetCommunityResponse_QNAME, GetCommunityResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetUrl }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "getUrl")
    public JAXBElement<GetUrl> createGetUrl(GetUrl value) {
        return new JAXBElement<GetUrl>(_GetUrl_QNAME, GetUrl.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetNextIdResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "getNextIdResponse")
    public JAXBElement<GetNextIdResponse> createGetNextIdResponse(GetNextIdResponse value) {
        return new JAXBElement<GetNextIdResponse>(_GetNextIdResponse_QNAME, GetNextIdResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetFileInfoResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "getFileInfoResponse")
    public JAXBElement<GetFileInfoResponse> createGetFileInfoResponse(GetFileInfoResponse value) {
        return new JAXBElement<GetFileInfoResponse>(_GetFileInfoResponse_QNAME, GetFileInfoResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UpdateKnownServerListResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "updateKnownServerListResponse")
    public JAXBElement<UpdateKnownServerListResponse> createUpdateKnownServerListResponse(UpdateKnownServerListResponse value) {
        return new JAXBElement<UpdateKnownServerListResponse>(_UpdateKnownServerListResponse_QNAME, UpdateKnownServerListResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetNextId }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "getNextId")
    public JAXBElement<GetNextId> createGetNextId(GetNextId value) {
        return new JAXBElement<GetNextId>(_GetNextId_QNAME, GetNextId.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PullFileResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tp2/", name = "pullFileResponse")
    public JAXBElement<PullFileResponse> createPullFileResponse(PullFileResponse value) {
        return new JAXBElement<PullFileResponse>(_PullFileResponse_QNAME, PullFileResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link byte[]}{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "arg2", scope = PushFile.class)
    public JAXBElement<byte[]> createPushFileArg2(byte[] value) {
        return new JAXBElement<byte[]>(_PushFileArg2_QNAME, byte[].class, PushFile.class, ((byte[]) value));
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link byte[]}{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "return", scope = PullFileResponse.class)
    public JAXBElement<byte[]> createPullFileResponseReturn(byte[] value) {
        return new JAXBElement<byte[]>(_PullFileResponseReturn_QNAME, byte[].class, PullFileResponse.class, ((byte[]) value));
    }

}
