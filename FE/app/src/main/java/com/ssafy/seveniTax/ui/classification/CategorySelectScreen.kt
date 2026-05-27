package com.ssafy.seveniTax.ui.classification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ssafy.seveniTax.ui.components.TaxHeader
import com.ssafy.seveniTax.ui.theme.*

data class TaxCategory(
    val name: String,
    val description: String = ""
)

data class TaxCategoryGroup(
    val initial: String,
    val categories: List<String>
)

@Composable
fun CategorySelectScreen(
    navController: NavController,
    currentCategory: String = "복리후생비",
    onCategorySelected: (String) -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf(currentCategory) }
    var searchQuery by remember { mutableStateOf("") }

    val frequentCategories = listOf(
        TaxCategory("복리후생비", "카페, 식사, 간식"),
        TaxCategory("접대비", "거래처 식사, 선물"),
        TaxCategory("여비교통비", "택시, 주유, 주차")
    )

    val allCategoryGroups = listOf(
        TaxCategoryGroup("ㄱ", listOf("감가상각비", "광고선전비", "교육훈련비")),
        TaxCategoryGroup("ㄷ", listOf("도서인쇄비")),
        TaxCategoryGroup("ㅂ", listOf("보험료", "복리후생비")),
        TaxCategoryGroup("ㅅ", listOf("세금과공과", "소모품비", "수선비", "수도광열비")),
        TaxCategoryGroup("ㅇ", listOf("여비교통비", "운반비", "임차료")),
        TaxCategoryGroup("ㅈ", listOf("접대비", "지급수수료")),
        TaxCategoryGroup("ㅊ", listOf("차량유지비")),
        TaxCategoryGroup("ㅌ", listOf("통신비"))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Background)
    ) {
        TaxHeader(
            title = "경비 변경",
            onBack = { navController.popBackStack() }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 28.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // 페이지 타이틀
            Text(
                text = "경비를 선택해주세요",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF343434)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 검색바
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // 자주 사용한 경비
                Text(
                    text = "자주 사용한 경비",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF898989)
                )

                Spacer(modifier = Modifier.height(8.dp))

                frequentCategories
                    .filter { searchQuery.isEmpty() || it.name.contains(searchQuery) }
                    .forEach { category ->
                        FrequentCategoryItem(
                            category = category,
                            isSelected = selectedCategory == category.name,
                            onClick = { selectedCategory = category.name }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                Spacer(modifier = Modifier.height(20.dp))

                // 전체 경비
                Text(
                    text = "전체 경비",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF898989)
                )

                Spacer(modifier = Modifier.height(12.dp))

                allCategoryGroups.forEach { group ->
                    val filteredCategories = group.categories.filter {
                        searchQuery.isEmpty() || it.contains(searchQuery)
                    }
                    if (filteredCategories.isNotEmpty()) {
                        // 초성 헤더
                        Text(
                            text = group.initial,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LogoPurple
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = Surface, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        filteredCategories.forEach { categoryName ->
                            Text(
                                text = categoryName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selectedCategory == categoryName) BrandPurple else Color(0xFF343434),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCategory = categoryName }
                                    .padding(vertical = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        // 하단 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 20.dp)
        ) {
            Button(
                onClick = { onCategorySelected(selectedCategory) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPurple,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "선택 완료",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        placeholder = {
            Text(
                text = "경비 검색",
                fontSize = 14.sp,
                color = Color(0xFFA8A3D7)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "검색",
                tint = Color(0xFFA8A3D7),
                modifier = Modifier.size(20.dp)
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Surface,
            unfocusedContainerColor = Surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
private fun FrequentCategoryItem(
    category: TaxCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                color = if (isSelected) Surface else Background,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.5.dp,
                color = if (isSelected) BrandPurple else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = category.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF343434)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (category.description.isNotEmpty()) {
                Text(
                    text = category.description,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF898989)
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(
                            color = BrandPurple,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "선택됨",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategorySelectScreenPreview() {
    CategorySelectScreen(
        navController = rememberNavController()
    )
}
