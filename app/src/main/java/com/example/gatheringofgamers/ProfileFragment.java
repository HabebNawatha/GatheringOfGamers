package com.example.gatheringofgamers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private static final int GALLERY_REQUEST_CODE = 123; // You can choose any integer here
//    private static final int CAMERA_REQUEST_CODE = 124; // You can choose any integer here



    private ViewPager viewPager;
    Uri image;
    private ProfilePagerAdapter pagerAdapter;
    private FirebaseFirestore db;
    private ImageView profile_img;
    private String username;

    private String userId;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance(String userId) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile, container, false);

        viewPager = view.findViewById(R.id.view_pager);
        pagerAdapter = new ProfilePagerAdapter(getChildFragmentManager(), userId);
        viewPager.setAdapter(pagerAdapter);
        profile_img = view.findViewById(R.id.profile_picture);

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);
        DocumentReference usersRef = db.collection("users").document(userId);
        usersRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    username = documentSnapshot.getString("username");
                    TextView profileName = view.findViewById(R.id.profile_name);
                    profileName.setText(username);

                    String encodedImage = documentSnapshot.getString("image");

                    if (encodedImage != null) {
                        // Decode the Base64 string and convert it into a Bitmap
                        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        // Set the Bitmap in the ImageView
                        profile_img.setImageBitmap(decodedByte);
                    }
                } else {
                    username = "Not Found!";
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                username = "Error";
            }
        });


        // Declare the editProfilePictureButton
        Button editProfilePictureButton = view.findViewById(R.id.editProfilePictureButton);

        // new is below
        editProfilePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // create a dialog here to let the user choose between taking a new photo or choosing from gallery
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Choose a picture");
                builder.setMessage("Would you like to take a new picture or choose one from your gallery?");
                builder.setPositiveButton("Take Photo", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //TODO Take Photo
                    }
                });
                builder.setNegativeButton("Choose from Gallery", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
                        photoPickerIntent.setType("image/*");
                        startActivityForResult(photoPickerIntent, GALLERY_REQUEST_CODE);
                    }
                });
                builder.show();
            }
        });


        return view;
    }
//    private File createImageFile() throws IOException {
//        // Create an image file name
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        String imageFileName = "JPEG_" + timeStamp + "_";
//        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//        File image = File.createTempFile(
//                imageFileName,  /* prefix */
//                ".jpg",         /* suffix */
//                storageDir      /* directory */
//        );
//        return image;
//    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch(requestCode) {
            case GALLERY_REQUEST_CODE:
                if(resultCode == Activity.RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    profile_img.setImageURI(selectedImage);
                    image = selectedImage;

                    // Convert the image to a Base64 string
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), selectedImage);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        byte[] bytes = baos.toByteArray();
                        String encodedImage = Base64.encodeToString(bytes, Base64.DEFAULT);

                        // Save the Base64 string to Firestore
                        Map<String, Object> user = new HashMap<>();
                        user.put("image", encodedImage);
                        db.collection("users").document(userId).update(user);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
//                break;
//            case CAMERA_REQUEST_CODE:
//                if(resultCode == Activity.RESULT_OK){
//                    // The image is saved to the given Uri
//                    Bitmap bitmap = null;
//                    try {
//                        bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), photoUri);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    profile_img.setImageBitmap(bitmap);
//
//                    // Convert the image to a Base64 string
//                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//                    byte[] bytes = baos.toByteArray();
//                    String encodedImage = Base64.encodeToString(bytes, Base64.DEFAULT);
//
//                    // Save the Base64 string to Firestore
//                    Map<String, Object> user = new HashMap<>();
//                    user.put("image", encodedImage);
//                    db.collection("users").document(userId).update(user);
//                }
//                break;
        }
    }


}
