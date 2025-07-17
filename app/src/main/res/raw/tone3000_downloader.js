piPedalDownloader.log("Start of script block.");

function piPedal_download_blob(blobUrl, mimeType, handle) {
    if (!piPedalDownloader) {
        console.error("piPedalDownloader is not defined");
        return;
    }
    piPedalDownloader.startDownload(handle);
    // read blob as a stream


    fetch(blobUrl)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.blob();
        })
        .then(blob => {
            if (!blob) {
                throw new Error("Blob is null or undefined");
            }
            if (blob.size === 0) {
                throw new Error("Blob is empty");
            }
            // Convert blob to ArrayBuffer
            const reader = new FileReader();
            reader.onloadend = function () {
                let result = reader.result;

                piPedalDownloader.write(handle, result);
                piPedalDownloader.endDownload(handle);
            };
            reader.readAsDataURL(blob);
        })
        .catch(error => {
            piPedalDownloader.error(handle, error.message);
        });

}

piPedalDownloader.log("End of script block.");

// Config package com.google.android.gms.clearcut_client#com.twoplay.pipedal cannot use FILE backing without declarative registration. See go/phenotype-android-integration#phenotype for more information. This will lead to stale flags.
//2025-07-16 12:52:16.226 21212-21487 FilePhenotypeFlags      com.twoplay.pipedal                  E  
// Config package com.google.android.gms.clearcut_client#com.twoplay.pipedal cannot use FILE backing without declarative registration. 
// See go/phenotype-android-integration#phenotype for more information. This will lead to stale flags.
//2025-07-16 12:52:16.325 21212-21362 Parcel                  com.twoplay.pipedal
//                   W  Expecting binder but got null!
//2025-07-16 12:52:16.358 21212-21298 OpenGLRenderer          com.twoplay.pipedal                  E  Unable to match the desired swap behavior.