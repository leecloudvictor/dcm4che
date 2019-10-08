//
///////////////////////////////////////////////////////////////
//                C O P Y R I G H T  (c) 2019                //
//        Agfa HealthCare N.V. and/or its affiliates         //
//                    All Rights Reserved                    //
///////////////////////////////////////////////////////////////
//                                                           //
//       THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF      //
//        Agfa HealthCare N.V. and/or its affiliates.        //
//      The copyright notice above does not evidence any     //
//     actual or intended publication of such source code.   //
//                                                           //
///////////////////////////////////////////////////////////////
//
package org.dcm4che3.io.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Open an image input stream based on the type of the input object.
 * @author Andrew Cowan (andrew.cowan@agfa.com)
 */
public class ServiceImageInputStreamLoader<T> implements ImageInputStreamLoader<T> {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceImageInputStreamLoader.class);

    static {
        // Ensure that the default behaviour is available if the service loaders were not visible in classpath
        if( !isInputClassHandled(URI.class) ) {
            IIORegistry.getDefaultInstance().registerServiceProvider(new FileURIImageInputStreamSpi());
        }

        if (!isInputClassHandled(ImageInputStream.class)) {
            IIORegistry.getDefaultInstance().registerServiceProvider(new DelegateInputStreamSpi());
        }
    }


    @Override
    public ImageInputStream openStream(T input) throws IOException {
        if(input == null) {
            throw new IllegalArgumentException("ImageInputStream cannot be bound to null input");
        }

        ServiceRegistry.Filter filter = filterImageInputStreamByInputClass(input.getClass());
        Iterator<ImageInputStreamSpi> it = IIORegistry.getDefaultInstance().getServiceProviders(ImageInputStreamSpi.class, filter,true);
        if(!it.hasNext()) {
            throw new IllegalArgumentException("No ImageInputStream SPI for type "+input.getClass() );
        }

        // Use the highest ordered SPI for this type
        ImageInputStreamSpi spi = it.next();
        return spi.createInputStreamInstance(input);
    }

    protected List<ImageInputStreamSpi> filterByType(Class type) {
        List<ImageInputStreamSpi> iisList = new ArrayList<>();

        Iterator<ImageInputStreamSpi> it = IIORegistry.getDefaultInstance().getServiceProviders(ImageInputStreamSpi.class, true);
        while (it.hasNext()) {
            ImageInputStreamSpi creator = it.next();
            if(creator.getInputClass().isAssignableFrom(type)) {
                iisList.add(creator);
            }
        }

        return iisList;
    }

    public static ServiceRegistry.Filter filterImageInputStreamByInputClass(Class inputClass) {
        return provider -> isImageInputStreamSpi(provider) && ((ImageInputStreamSpi)provider).getInputClass().isAssignableFrom(inputClass);
    }

    private static boolean isImageInputStreamSpi(Object provider) {
        return provider instanceof ImageInputStreamSpi;
    }

    private static boolean isInputClassHandled(Class inputClass) {
        ServiceRegistry.Filter filter = filterImageInputStreamByInputClass(inputClass);
        Iterator<ImageInputStreamSpi> it = IIORegistry.getDefaultInstance().getServiceProviders(ImageInputStreamSpi.class, filter,false);
        return it.hasNext();

    }
}
