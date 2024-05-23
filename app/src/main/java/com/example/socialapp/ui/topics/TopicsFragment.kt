package com.example.socialapp.ui.topics

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.size.ViewSizeResolver
import com.example.socialapp.databinding.TopicsFragmentBinding
import com.example.socialapp.pager.updateTopicPhotosStatus
import com.example.socialapp.topicViewModelKey
import com.example.socialapp.ui.topicphotos.TopicPhotosActivity
import com.example.socialapp.viewmodel.Status
import com.example.socialapp.viewmodel.TopicViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

var topicAdapter: TopicAdapter? = null
@AndroidEntryPoint
class TopicsFragment : Fragment() {

    private var _binding: TopicsFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TopicsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val topicViewModel =
            ViewModelProvider(requireActivity())[topicViewModelKey, TopicViewModel::class.java]

        val progressBar = binding.progressBar
        val statusIcon = binding.status

        val statusObserver = Observer<Status> { status ->
            when (status) {
                Status.LOADING -> {
                    statusIcon.visibility = View.INVISIBLE
                    progressBar.visibility = View.VISIBLE
                }

                Status.SUCCESS -> {
                    statusIcon.visibility = View.INVISIBLE
                    progressBar.visibility = View.INVISIBLE
                }

                else -> {
                    progressBar.visibility = View.INVISIBLE
                    statusIcon.visibility = View.VISIBLE
                }
            }
        }

        topicViewModel.topicsStatus.observe(viewLifecycleOwner, statusObserver)

        val metrics: DisplayMetrics = requireContext().resources.displayMetrics

        topicAdapter = TopicAdapter(metrics) { holder, _, data ->
            val coverPhoto = holder.coverPhoto
            val text = holder.topicText
            val topicItem = holder.topicItem
            if (coverPhoto != null && text != null && topicItem != null) {

                coverPhoto.clipToOutline = true

                val url = data.cover_photo?.urls?.regular

                coverPhoto.load(url){
                    size(ViewSizeResolver(coverPhoto))
                }

                text.text = data.title

                topicItem.setOnClickListener {
                    updateTopicPhotosStatus = true
                    val arg = Bundle().apply {
                        putString("slug", "${data.slug}")
                        putString("title", "${data.title}")
                    }

                    val intent = Intent(activity, TopicPhotosActivity::class.java)
                    intent.putExtras(arg)
                    ActivityCompat.startActivity(requireActivity(), intent, null)
                }
            }
        }

        lifecycleScope.launch {
            topicAdapter!!.data = topicViewModel.getTopics()
        }

        binding.recyclerView.adapter = topicAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}