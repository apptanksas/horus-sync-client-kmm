package org.apptank.horus.client.sync.manager

import io.mockative.Mock
import io.mockative.classOf
import io.mockative.mock
import org.apptank.horus.client.di.INetworkValidator
import org.apptank.horus.client.sync.upload.repository.IUploadFileRepository
import org.junit.Before


class SyncFileUploadedManagerTest {

    @Mock
    private val networkValidator: INetworkValidator = mock(classOf<INetworkValidator>())

    @Mock
    private val repository: IUploadFileRepository = mock(classOf<IUploadFileRepository>())

    private lateinit var manager: SyncFileUploadedManager

    @Before
    fun setUp() {
        manager = SyncFileUploadedManager(
            networkValidator,
            repository
        )
    }


}