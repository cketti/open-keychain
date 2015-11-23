package org.sufficientlysecure.keychain.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.openintents.openpgp.OpenPgpMetadata;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.sufficientlysecure.keychain.WorkaroundBuildConfig;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.InputDataResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcelHelper;
import org.sufficientlysecure.keychain.service.InputDataParcel;
import org.sufficientlysecure.keychain.ui.DecryptListInterface.DecryptListView;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = WorkaroundBuildConfig.class, sdk = 21, manifest = "src/main/AndroidManifest.xml")
public class DecryptListPresenterTest {
    private static final Uri DUMMY_URI = Uri.parse("content://org.example.dummy/1");

    private DecryptListView mView;
    private TestDecryptListPresenter mPresenter;

    @Before
    public void setUp() throws Exception {
        mView = createFakeView();
        mPresenter = new TestDecryptListPresenter();
    }

    @Test
    public void initialize_withCanDeleteFalse_shouldSetShowDeleteFileFalse() throws Exception {
        initializePresenter(false);

        verify(mView).setShowDeleteFile(false);
    }

    @Test
    public void initialize_withCanDeleteTrue_shouldSetShowDeleteFileTrue() throws Exception {
        initializePresenter(true);

        verify(mView).setShowDeleteFile(true);
    }

    @Test
    public void initialize_withSingleInputUri_shouldCallAddOnce() throws Exception {
        initializePresenterWithSingleInputUri();

        verify(mView, times(1)).addItem();
    }

    @Test
    public void initialize_withThreeInputUris_shouldCallAddThreeTimes() throws Exception {
        Uri uriOne = Uri.parse("content://org.example.dummy/1");
        Uri uriTwo = Uri.parse("content://org.example.dummy/2");
        Uri uriThree = Uri.parse("content://org.example.dummy/3");
        initializePresenter(true, null, uriOne, uriTwo, uriThree);

        verify(mView, times(3)).addItem();
    }

    @Test
    public void initialize_shouldCallCryptoOperation() throws Exception {
        initializePresenterWithSingleInputUri();

        verify(mPresenter.mCryptoHelper).cryptoOperation();
        verifyNoMoreInteractions(mPresenter.mCryptoHelper);
    }

    @Test
    public void detachView_shouldCancelAllCryptoHelperOperations() throws Exception {
        initializePresenterWithSingleInputUri();

        mPresenter.detachView();

        verify(mPresenter.mCryptoHelper).cancelAllOperations();
    }

    @Test
    public void createOperationInput_withSingleUri_shouldReturnExpectedResult() throws Exception {
        initializePresenterWithSingleInputUri();

        InputDataParcel operationInput = mPresenter.createOperationInput();

        assertNotNull(operationInput);
        assertEquals(DUMMY_URI, operationInput.getInputUri());
        assertTrue(PgpDecryptVerifyInputParcelHelper.isAllowSymmetricDecryption(operationInput.getDecryptInput()));
    }

    @Test
    public void onUiClickFile_shouldStartViewIntentWithOutputUri() throws Exception {
        Uri outputUri = Uri.parse("content://output/1");
        initializeAndDecrypt(outputUri);

        mPresenter.onUiClickFile(0, 0);

        Intent intent = getStartActivityIntent();
        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals("text/plain", intent.getType());
        assertEquals(outputUri, intent.getData());
    }

    private DecryptListView createFakeView() {
        DecryptListView fakeView = mock(DecryptListView.class);
        FragmentActivity fakeActivity = createFakeActivity();
        when(fakeView.getActivity()).thenReturn(fakeActivity);
        return fakeView;
    }

    private FragmentActivity createFakeActivity() {
        FragmentActivity fakeActivity = mock(FragmentActivity.class);
        when(fakeActivity.getResources()).thenReturn(RuntimeEnvironment.application.getResources());
        return fakeActivity;
    }

    private void initializePresenter(boolean canDelete) {
        initializePresenter(canDelete, null, DUMMY_URI);
    }

    private void initializePresenter(boolean canDelete, Bundle savedInstanceState,
            Uri... inputUris) {
        mPresenter.attachView(mView);

        ArrayList<Uri> inputUriList = new ArrayList<>(Arrays.asList(inputUris));
        mPresenter.initialize(inputUriList, canDelete, savedInstanceState);
    }

    private void initializePresenterWithSingleInputUri() {
        initializePresenter(false);
    }

    private void initializeAndDecrypt(Uri outputUri) {
        initializePresenterWithSingleInputUri();
        mPresenter.createOperationInput();
        InputDataResult cryptoResult = createCryptoResult(outputUri);
        mPresenter.onCryptoOperationSuccess(cryptoResult);
    }

    private InputDataResult createCryptoResult(Uri... uris) {
        OperationLog log = new OperationLog();
        DecryptVerifyResult decryptResult = new DecryptVerifyResult(
                DecryptVerifyResult.RESULT_OK, log);

        ArrayList<Uri> outputUris = new ArrayList<>();
        ArrayList<OpenPgpMetadata> metadataList = new ArrayList<>();
        for (int i = 0, end = uris.length; i < end; i++) {
            outputUris.add(uris[i]);
            metadataList.add(createMetadata(i));
        }

        return new InputDataResult(InputDataResult.RESULT_OK, log, decryptResult, outputUris,
                metadataList);
    }

    private OpenPgpMetadata createMetadata(int i) {
        String filename = "filename-" + i;
        int modificationTime = 0;
        int originalSize = 42;

        return new OpenPgpMetadata(filename, "text/plain", modificationTime, originalSize);
    }

    private Intent getStartActivityIntent() {
        ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mView.getActivity()).startActivity(argumentCaptor.capture());
        return argumentCaptor.getValue();
    }


    static class TestDecryptListPresenter extends DecryptListPresenter {
        public CryptoOperationHelper<InputDataParcel, InputDataResult> mCryptoHelper;

        @NonNull
        @Override
        protected CryptoOperationHelper<InputDataParcel, InputDataResult> createCryptoOperationHelper(
                DecryptListView decryptListView) {
            mCryptoHelper = createFakeCryptoOperationHelper();
            return mCryptoHelper;
        }

        @SuppressWarnings("unchecked")
        private CryptoOperationHelper<InputDataParcel, InputDataResult> createFakeCryptoOperationHelper() {
            return mock(CryptoOperationHelper.class);
        }
    }
}
